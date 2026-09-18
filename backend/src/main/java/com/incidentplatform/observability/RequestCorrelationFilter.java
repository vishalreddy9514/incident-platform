package com.incidentplatform.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts a request ID in both the response ({@code X-Request-Id}) and the logging MDC, so every log
 * line the JSON-structured logs emit while handling one request - across every layer, including the
 * AI-service call in {@link com.incidentplatform.ai} - can be grepped/filtered together by a single
 * field. Reuses an inbound {@code X-Request-Id} if the caller already set one (e.g. an ALB or
 * another service), rather than always minting a fresh one, so a trace started upstream isn't
 * broken here.
 *
 * <p>Runs first in the chain (see {@code SecurityConfig}) so the ID is available to every other
 * filter's own logging, not just application code.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    response.setHeader(REQUEST_ID_HEADER, requestId);
    MDC.put(MDC_KEY, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
