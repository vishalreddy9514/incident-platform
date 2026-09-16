package com.incidentplatform.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.auth.dto.AuthResponse;
import com.incidentplatform.auth.dto.LoginRequest;
import com.incidentplatform.auth.dto.RegisterRequest;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.incident.dto.AssignRequest;
import com.incidentplatform.incident.dto.CommentRequest;
import com.incidentplatform.incident.dto.CreateIncidentRequest;
import com.incidentplatform.incident.dto.IncidentDetailResponse;
import com.incidentplatform.incident.dto.UpdateIncidentRequest;
import com.incidentplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end test of the core incident workflow (FR-5 to FR-12) against real Postgres and Redis
 * containers: create → ownership-based view rules → status transitions (valid and invalid) →
 * assignment → comments → history — the genuine proof that the state machine and RBAC rules in
 * {@link IncidentService} hold up over real HTTP requests, not just mocked unit tests.
 *
 * <p>There's no admin-promotion endpoint reachable from a fresh account, so this test promotes a
 * registered user to ENGINEER directly via {@link UserRepository} — legitimate for test setup,
 * distinct from {@code AdminBootstrapRunner}'s production bootstrapping mechanism. Tokens issued
 * before a role change still carry the old role (JWT claims are a snapshot at issue time), so each
 * promotion is followed by a fresh login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IncidentManagementIntegrationTest {

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
  @Autowired private UserRepository userRepository;

  private final TestRestTemplate restTemplate = new TestRestTemplate();
  private int userCounter = 0;

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private String uniqueEmail(String label) {
    return label + "-" + (userCounter++) + "-" + System.nanoTime() + "@example.com";
  }

  private String register(String email) {
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            new RegisterRequest(email, "password123", "Test User"),
            AuthResponse.class);
    return response.getBody().accessToken();
  }

  private String login(String email) {
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity(
            url("/api/v1/auth/login"), new LoginRequest(email, "password123"), AuthResponse.class);
    return response.getBody().accessToken();
  }

  /**
   * Registers a user, promotes them to ENGINEER directly via the repository, then logs in again to
   * get a token that actually carries the new role.
   */
  private String registerEngineer(String email) {
    register(email);
    var user = userRepository.findByEmail(email).orElseThrow();
    user.setRole(Role.ENGINEER);
    userRepository.save(user);
    return login(email);
  }

  private HttpHeaders authHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createIncident(String token) {
    ResponseEntity<IncidentDetailResponse> response =
        restTemplate.postForEntity(
            url("/api/v1/incidents"),
            new HttpEntity<>(
                new CreateIncidentRequest("Laptop won't boot", "Black screen on startup", 1L),
                authHeaders(token)),
            IncidentDetailResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody().id();
  }

  @Test
  void aUserCanCreateAnIncidentAndViewItButNotAnotherUsersIncident() {
    String ownerToken = register(uniqueEmail("owner"));
    String strangerToken = register(uniqueEmail("stranger"));

    Long incidentId = createIncident(ownerToken);

    ResponseEntity<IncidentDetailResponse> ownerViewResponse =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(ownerToken)),
            IncidentDetailResponse.class);
    assertThat(ownerViewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ownerViewResponse.getBody().status()).isEqualTo(IncidentStatus.OPEN);

    ResponseEntity<String> strangerViewResponse =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(strangerToken)),
            String.class);
    assertThat(strangerViewResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void aRegularUserCannotChangeStatusButAnEngineerCanThroughAValidTransition() {
    String ownerToken = register(uniqueEmail("reporter"));
    String engineerToken = registerEngineer(uniqueEmail("engineer"));
    Long incidentId = createIncident(ownerToken);

    ResponseEntity<String> userAttempt =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId),
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateIncidentRequest(null, null, null, IncidentStatus.IN_PROGRESS, null, null),
                authHeaders(ownerToken)),
            String.class);
    assertThat(userAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<IncidentDetailResponse> engineerUpdate =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId),
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateIncidentRequest(null, null, null, IncidentStatus.IN_PROGRESS, null, null),
                authHeaders(engineerToken)),
            IncidentDetailResponse.class);
    assertThat(engineerUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(engineerUpdate.getBody().status()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }

  @Test
  void anInvalidStatusTransitionIsRejectedWithConflict() {
    String ownerToken = register(uniqueEmail("reporter2"));
    String engineerToken = registerEngineer(uniqueEmail("engineer2"));
    Long incidentId = createIncident(ownerToken);

    // IN_PROGRESS -> CLOSED is not a valid direct transition (must go through RESOLVED).
    restTemplate.exchange(
        url("/api/v1/incidents/" + incidentId),
        HttpMethod.PUT,
        new HttpEntity<>(
            new UpdateIncidentRequest(null, null, null, IncidentStatus.IN_PROGRESS, null, null),
            authHeaders(engineerToken)),
        IncidentDetailResponse.class);

    ResponseEntity<String> invalidTransition =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId),
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateIncidentRequest(null, null, null, IncidentStatus.CLOSED, null, null),
                authHeaders(engineerToken)),
            String.class);

    assertThat(invalidTransition.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(invalidTransition.getBody()).contains("INVALID_STATUS_TRANSITION");
  }

  @Test
  void anEngineerCanBeAssignedAndTheIncidentMovesToInProgress() {
    String ownerToken = register(uniqueEmail("assign-reporter"));
    String engineerEmail = uniqueEmail("assign-engineer");
    String engineerToken = registerEngineer(engineerEmail);
    var engineerUser = userRepository.findByEmail(engineerEmail).orElseThrow();

    Long incidentId = createIncident(ownerToken);

    ResponseEntity<IncidentDetailResponse> assignResponse =
        restTemplate.postForEntity(
            url("/api/v1/incidents/" + incidentId + "/assign"),
            new HttpEntity<>(new AssignRequest(engineerUser.getId()), authHeaders(engineerToken)),
            IncidentDetailResponse.class);

    assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(assignResponse.getBody().assignedToId()).isEqualTo(engineerUser.getId());
    assertThat(assignResponse.getBody().status()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }

  @Test
  void commentsAndHistoryAreRecordedAndVisibleToTheOwner() {
    String ownerToken = register(uniqueEmail("commenter"));
    Long incidentId = createIncident(ownerToken);

    ResponseEntity<String> commentResponse =
        restTemplate.postForEntity(
            url("/api/v1/incidents/" + incidentId + "/comments"),
            new HttpEntity<>(
                new CommentRequest("Tried a hard reset, no luck."), authHeaders(ownerToken)),
            String.class);
    assertThat(commentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(commentResponse.getBody()).contains("Tried a hard reset");

    ResponseEntity<String> historyResponse =
        restTemplate.exchange(
            url("/api/v1/incidents/" + incidentId + "/history"),
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(ownerToken)),
            String.class);
    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Creation itself records a "status: null -> OPEN" history entry.
    assertThat(historyResponse.getBody()).contains("\"fieldChanged\":\"status\"");
  }
}
