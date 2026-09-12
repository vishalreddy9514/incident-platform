package com.incidentplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Runs when an unauthenticated request hits a protected endpoint. Without this, Spring Security's
 * default behaviour returns an empty 401 (or, with HTTP Basic active, a browser auth prompt) —
 * neither matches the standard error envelope every other error path in this API uses.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse body =
        ErrorResponse.of("UNAUTHORIZED", "Authentication is required to access this resource");
    objectMapper.writeValue(response.getWriter(), body);
  }
}
