package com.incidentplatform.dashboard;

import com.incidentplatform.dashboard.dto.DashboardMetricsResponse;
import com.incidentplatform.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/metrics")
  public DashboardMetricsResponse getMetrics(@AuthenticationPrincipal CustomUserDetails principal) {
    return dashboardService.getMetrics(principal);
  }
}
