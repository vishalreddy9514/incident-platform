package com.incidentplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private RateLimitingFilter filter() {
    return new RateLimitingFilter(redisTemplate, objectMapper, 5, 60);
  }

  @Test
  void allowsRequestsUnderTheLimit() throws Exception {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("rate-limit:/api/v1/auth/login:127.0.0.1")).thenReturn(3L);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(response.getStatus()).isEqualTo(200); // MockHttpServletResponse default
  }

  @Test
  void blocksRequestsOverTheLimitWithA429() throws Exception {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("rate-limit:/api/v1/auth/login:127.0.0.1")).thenReturn(6L);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentAsString()).contains("RATE_LIMITED");
  }

  @Test
  void doesNotRateLimitUnrelatedPaths() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(redisTemplate);
  }
}
