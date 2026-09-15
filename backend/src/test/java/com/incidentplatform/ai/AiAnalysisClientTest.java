package com.incidentplatform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.incidentplatform.ai.dto.AiAnalysisResult;
import com.incidentplatform.ai.dto.AnalyseIncidentRequest;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.domain.incident.IncidentPriority;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Exercises {@link AiAnalysisClient} against a {@link MockRestServiceServer} — a real HTTP request
 * is built and matched (headers, path, body), just without a real network call, rather than mocking
 * the client away entirely.
 */
class AiAnalysisClientTest {

  private MockRestServiceServer server;
  private AiAnalysisClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new AiAnalysisClient(builder, "http://ai-service:8000", "test-internal-token");
  }

  @Test
  void sendsTheInternalTokenAndRequestBodyAndParsesTheResponse() {
    server
        .expect(requestTo("http://ai-service:8000/internal/v1/analyse"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Internal-Token", "test-internal-token"))
        .andExpect(
            content()
                .json(
                    """
            {"title":"VPN down","description":"Cannot connect","category":"Network"}
            """))
        .andRespond(
            withSuccess(
                """
                {
                  "suggestedCategory": "Network",
                  "predictedPriority": "HIGH",
                  "summary": "User cannot connect to the VPN.",
                  "keywords": ["vpn", "connection"],
                  "suggestedSteps": ["Restart VPN client"],
                  "modelUsed": "mock-heuristic-v1"
                }
                """,
                MediaType.APPLICATION_JSON));

    AiAnalysisResult result =
        client.analyse(new AnalyseIncidentRequest("VPN down", "Cannot connect", "Network"));

    assertThat(result.suggestedCategory()).isEqualTo("Network");
    assertThat(result.predictedPriority()).isEqualTo(IncidentPriority.HIGH);
    assertThat(result.keywords()).containsExactly("vpn", "connection");
    assertThat(result.modelUsed()).isEqualTo("mock-heuristic-v1");
    server.verify();
  }

  @Test
  void wrapsANon2xxResponseAsServiceUnavailable() {
    server
        .expect(requestTo("http://ai-service:8000/internal/v1/analyse"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> client.analyse(new AnalyseIncidentRequest("t", "d", null)))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("AI_SERVICE_UNAVAILABLE");
  }

  @Test
  void wrapsAConnectionFailureAsServiceUnavailable() {
    server
        .expect(requestTo("http://ai-service:8000/internal/v1/analyse"))
        .andRespond(
            request -> {
              throw new ConnectException("Connection refused");
            });

    assertThatThrownBy(() -> client.analyse(new AnalyseIncidentRequest("t", "d", null)))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("AI_SERVICE_UNAVAILABLE");
  }
}
