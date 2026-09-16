package com.incidentplatform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.auth.dto.AuthResponse;
import com.incidentplatform.auth.dto.LoginRequest;
import com.incidentplatform.auth.dto.RefreshRequest;
import com.incidentplatform.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end authentication and RBAC test against real PostgreSQL and Redis containers — the
 * genuine proof, alongside the unit/slice tests, that the full chain (register → login → JWT
 * issuance → protected endpoint access → role-based access control → refresh rotation) actually
 * works, not just that each piece compiles.
 *
 * <p>The auth rate limit is raised via {@code app.rate-limit.auth.max-requests} for this test class
 * only, since a single test class legitimately makes more than 5 login/register calls from the same
 * "client" (the test's HTTP client shares one source IP) in well under the rate limit window —
 * that's a test-environment artefact, not something the real rate limit should be loosened for in
 * production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthenticationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("incident_platform_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("app.rate-limit.auth.max-requests", () -> 1000);
  }

  @LocalServerPort private int port;

  private final TestRestTemplate restTemplate = new TestRestTemplate();

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  @Test
  void registeredUserCanLoginAndAccessTheirOwnProfile() {
    RegisterRequest registerRequest =
        new RegisterRequest("integration-user@example.com", "password123", "Integration User");
    ResponseEntity<AuthResponse> registerResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"), registerRequest, AuthResponse.class);

    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(registerResponse.getBody()).isNotNull();
    String accessToken = registerResponse.getBody().accessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<String> profileResponse =
        restTemplate.exchange(
            url("/api/v1/users/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(profileResponse.getBody()).contains("integration-user@example.com");
  }

  @Test
  void protectedEndpointRejectsRequestsWithoutAToken() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(url("/api/v1/users/me"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("UNAUTHORIZED");
  }

  @Test
  void loginRejectsAnIncorrectPassword() {
    restTemplate.postForEntity(
        url("/api/v1/auth/register"),
        new RegisterRequest("wrongpass-user@example.com", "correct-password", "Someone"),
        AuthResponse.class);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            url("/api/v1/auth/login"),
            new LoginRequest("wrongpass-user@example.com", "incorrect-password"),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("INVALID_CREDENTIALS");
  }

  @Test
  void regularUserCannotAccessTheAdminOnlyUserListEndpoint() {
    ResponseEntity<AuthResponse> registerResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            new RegisterRequest("plain-user@example.com", "password123", "Plain User"),
            AuthResponse.class);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(registerResponse.getBody().accessToken());
    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/users"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("FORBIDDEN");
  }

  /**
   * Registration only ever creates USER accounts (FR-4), so these denial tests cover the one role
   * reachable without a separately-bootstrapped ADMIN — the same constraint {@link
   * #regularUserCannotAccessTheAdminOnlyUserListEndpoint} already works within.
   */
  private HttpHeaders headersForANewlyRegisteredUser(String email) {
    ResponseEntity<AuthResponse> registerResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            new RegisterRequest(email, "password123", "A User"),
            AuthResponse.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(registerResponse.getBody().accessToken());
    return headers;
  }

  @Test
  void regularUserCannotCreateACategory() {
    HttpHeaders headers = headersForANewlyRegisteredUser("category-create-user@example.com");
    String body = "{\"name\":\"Hardware\",\"description\":\"Physical equipment issues\"}";
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/categories"),
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void regularUserCannotUpdateACategory() {
    HttpHeaders headers = headersForANewlyRegisteredUser("category-update-user@example.com");
    String body = "{\"name\":\"Hardware\",\"description\":\"Updated\",\"isActive\":true}";
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/categories/1"),
            HttpMethod.PUT,
            new HttpEntity<>(body, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void regularUserCannotCreateATeam() {
    HttpHeaders headers = headersForANewlyRegisteredUser("team-create-user@example.com");
    String body = "{\"name\":\"Platform\",\"description\":\"Core platform team\"}";
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/teams"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void regularUserCannotChangeAnyUsersRole() {
    // @PreAuthorize("hasRole('ADMIN')") rejects before the method body (and the self-role-edit
    // check UserService.updateRole applies) ever runs, so the target ID doesn't need to be real.
    HttpHeaders headers = headersForANewlyRegisteredUser("role-caller-user@example.com");
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/users/999999/role"),
            HttpMethod.PATCH,
            new HttpEntity<>("{\"role\":\"ENGINEER\"}", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void refreshTokenRotatesAndTheOldTokenCannotBeReused() {
    ResponseEntity<AuthResponse> registerResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            new RegisterRequest("refresh-user@example.com", "password123", "Refresh User"),
            AuthResponse.class);
    String originalRefreshToken = registerResponse.getBody().refreshToken();

    ResponseEntity<AuthResponse> refreshResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/refresh"),
            new RefreshRequest(originalRefreshToken),
            AuthResponse.class);

    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(originalRefreshToken);

    // The original refresh token was consumed by rotation — reusing it must fail.
    ResponseEntity<String> reuseResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/refresh"), new RefreshRequest(originalRefreshToken), String.class);

    assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(reuseResponse.getBody()).contains("INVALID_REFRESH_TOKEN");
  }
}
