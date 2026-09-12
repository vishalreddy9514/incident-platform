package com.incidentplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Runs when an authenticated request is correctly identified but lacks permission (e.g. a USER
 * hitting an ADMIN-only endpoint via {@code @PreAuthorize}). Kept separate from {@link
 * RestAuthenticationEntryPoint} because "who are you" (401) and "you can't do that" (403) are
 * different failure modes with different client-side handling.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse body =
        ErrorResponse.of("FORBIDDEN", "You do not have permission to perform this action");
    objectMapper.writeValue(response.getWriter(), body);
  }
}
