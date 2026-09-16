package com.incidentplatform.dashboard;

import com.incidentplatform.dashboard.dto.DashboardMetricsResponse;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.repository.IncidentRepository;
import com.incidentplatform.security.CustomUserDetails;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Role-scoped dashboard metrics (FR-16/17/18), computed via a database {@code GROUP BY} (see {@link
 * IncidentRepository#countByStatus} / {@code countByStatusForAssignee}), per Phase 1 §6.3 UC-7's
 * explicit requirement: aggregate queries, not client-side computation over the full dataset.
 */
@Service
public class DashboardService {

  private final IncidentRepository incidentRepository;

  public DashboardService(IncidentRepository incidentRepository) {
    this.incidentRepository = incidentRepository;
  }

  public DashboardMetricsResponse getMetrics(CustomUserDetails principal) {
    Role role = principal.getUser().getRole();

    List<IncidentRepository.StatusCount> rows =
        switch (role) {
          case USER -> incidentRepository.countByStatus(principal.getUserId());
          case ENGINEER -> incidentRepository.countByStatusForAssignee(principal.getUserId());
          case ADMIN -> incidentRepository.countByStatus(null);
        };

    // Every status starts at 0 so the response always has a consistent, complete key set —
    // the frontend never needs to guard against a status simply not appearing yet.
    Map<String, Long> countsByStatus = new LinkedHashMap<>();
    for (IncidentStatus status : IncidentStatus.values()) {
      countsByStatus.put(status.name(), 0L);
    }
    for (IncidentRepository.StatusCount row : rows) {
      countsByStatus.put(row.getStatus().name(), row.getCount());
    }

    long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
    String scope =
        switch (role) {
          case USER -> "OWN";
          case ENGINEER -> "ASSIGNED";
          case ADMIN -> "SYSTEM_WIDE";
        };

    return new DashboardMetricsResponse(scope, total, countsByStatus);
  }
}
