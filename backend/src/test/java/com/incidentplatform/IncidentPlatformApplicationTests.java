package com.incidentplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.auth.dto.AuthResponse;
import com.incidentplatform.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The broadest possible check: boots the entire Spring context (all entities, repositories,
 * security config, exception handling) against real Postgres and Redis, on a real HTTP port. If any
 * bean is misconfigured or any entity doesn't match the migrated schema, this test fails to start.
 *
 * <p>Updated in Phase 5: {@code /api/v1/categories} now requires authentication (it no longer sits
 * in the temporary Phase 4 permit-all), so this test registers a real user and uses the issued
 * token — exercising the same JWT path a real client would, rather than special-casing the test
 * around the security change. See {@code AuthenticationIntegrationTest} for the fuller
 * authentication/RBAC test suite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IncidentPlatformApplicationTests {

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
  }

  @LocalServerPort private int port;

  private final TestRestTemplate restTemplate = new TestRestTemplate();

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  @Test
  void contextLoads() {
    // Intentionally empty: a failure to start the context fails this test.
  }

  @Test
  void categoriesEndpointRespondsWithSeededDataForAnAuthenticatedUser() {
    ResponseEntity<AuthResponse> registerResponse =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            new RegisterRequest("smoke-test-user@example.com", "password123", "Smoke Test"),
            AuthResponse.class);
    String accessToken = registerResponse.getBody().accessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/api/v1/categories"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("Hardware");
  }

  @Test
  void actuatorHealthEndpointReportsUp() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(url("/actuator/health"), String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("UP");
  }
}
