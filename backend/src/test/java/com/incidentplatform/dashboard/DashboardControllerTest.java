package com.incidentplatform.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incidentplatform.dashboard.dto.DashboardMetricsResponse;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.security.CustomUserDetails;
import com.incidentplatform.security.JwtAuthenticationFilter;
import com.incidentplatform.security.RateLimitingFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC slice test — security filters disabled, same rationale as {@code CategoryControllerTest}.
 * {@code getMetrics} needs an {@code @AuthenticationPrincipal CustomUserDetails}, so the
 * Authentication is set directly on {@link SecurityContextHolder} rather than relying on a request
 * post-processor — see {@code UserControllerTest} for why.
 */
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DashboardService dashboardService;
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private RateLimitingFilter rateLimitingFilter;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getMetricsReturnsTheScopedResponse() throws Exception {
    User user = new User("user@example.com", "hashed", "User", Role.USER);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    when(dashboardService.getMetrics(any()))
        .thenReturn(new DashboardMetricsResponse("OWN", 3L, Map.of("OPEN", 3L)));

    mockMvc
        .perform(get("/api/v1/dashboard/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scope").value("OWN"))
        .andExpect(jsonPath("$.totalIncidents").value(3))
        .andExpect(jsonPath("$.countsByStatus.OPEN").value(3));
  }
}
