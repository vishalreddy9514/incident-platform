package com.incidentplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple fixed-window rate limiter, applied only to the credential-guessing-sensitive auth
 * endpoints (login, register) per Phase 1 §10 — not applied globally, since most of the API
 * doesn't need it and blanket rate limiting would just be friction. Redis {@code INCR} + {@code
 * EXPIRE} gives an atomic-enough counter for this purpose without needing a dedicated rate-limit
 * library.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final Set<String> RATE_LIMITED_PATHS =
      Set.of("/api/v1/auth/login", "/api/v1/auth/register");

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final int maxRequests;
  private final Duration window;

  public RateLimitingFilter(
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.rate-limit.auth.max-requests}") int maxRequests,
      @Value("${app.rate-limit.auth.window-seconds}") long windowSeconds) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.maxRequests = maxRequests;
    this.window = Duration.ofSeconds(windowSeconds);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (RATE_LIMITED_PATHS.contains(request.getRequestURI())) {
      String key = "rate-limit:%s:%s".formatted(request.getRequestURI(), request.getRemoteAddr());
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, window);
      }
      if (count != null && count > maxRequests) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body =
            ErrorResponse.of("RATE_LIMITED", "Too many requests. Please try again later.");
        objectMapper.writeValue(response.getWriter(), body);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
