package com.incidentplatform.ai;

import com.incidentplatform.ai.dto.AiAnalysisResult;
import com.incidentplatform.ai.dto.AnalyseIncidentRequest;
import com.incidentplatform.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The single point of contact with the Python AI service (ADR-0001), authenticated with the shared
 * {@code X-Internal-Token} header (Phase 1 §10) that the AI service's own {@code
 * verify_internal_token} dependency checks.
 *
 * <p>Connect/read timeouts are applied globally to every injected {@link RestClient.Builder} by
 * {@code RestClientConfig}, not here — keeping that cross-cutting concern out of this class also
 * means a test can bind {@link org.springframework.test.web.client.MockRestServiceServer} directly
 * to a plain builder without this constructor overwriting its mock request factory.
 *
 * <p>Any failure to reach the service, a non-2xx response, or a response that doesn't match the
 * expected shape is translated into a single {@code 503 AI_SERVICE_UNAVAILABLE} — the incident
 * workflow itself never depends on this succeeding (FR-15); only the caller of {@code
 * AiAnalysisService.analyse} sees the failure.
 */
@Component
class AiAnalysisClient {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final RestClient restClient;

  AiAnalysisClient(
      RestClient.Builder builder,
      @Value("${app.ai-service.base-url}") String baseUrl,
      @Value("${app.ai-service.internal-token}") String internalToken) {
    this.restClient =
        builder.baseUrl(baseUrl).defaultHeader(INTERNAL_TOKEN_HEADER, internalToken).build();
  }

  AiAnalysisResult analyse(AnalyseIncidentRequest request) {
    try {
      return restClient
          .post()
          .uri("/internal/v1/analyse")
          .contentType(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
          .body(request)
          .retrieve()
          .body(AiAnalysisResult.class);
    } catch (RestClientException ex) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "AI_SERVICE_UNAVAILABLE",
          "The AI analysis service is currently unavailable",
          ex);
    }
  }
}
