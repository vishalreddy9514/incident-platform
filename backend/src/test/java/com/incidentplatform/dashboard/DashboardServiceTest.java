package com.incidentplatform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.IncidentRepository;
import com.incidentplatform.security.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock private IncidentRepository incidentRepository;

  private IncidentRepository.StatusCount statusCount(IncidentStatus status, long count) {
    IncidentRepository.StatusCount row = mock(IncidentRepository.StatusCount.class);
    when(row.getStatus()).thenReturn(status);
    when(row.getCount()).thenReturn(count);
    return row;
  }

  @Test
  void userScopeReturnsOwnCountsWithEveryStatusPresent() {
    User user = new User("user@example.com", "hashed", "User", Role.USER);
    CustomUserDetails principal = new CustomUserDetails(user);
    when(incidentRepository.countByStatus(principal.getUserId()))
        .thenReturn(List.of(statusCount(IncidentStatus.OPEN, 3)));

    var response = new DashboardService(incidentRepository).getMetrics(principal);

    assertThat(response.scope()).isEqualTo("OWN");
    assertThat(response.countsByStatus()).hasSize(5); // every IncidentStatus value
    assertThat(response.countsByStatus().get("OPEN")).isEqualTo(3L);
    assertThat(response.countsByStatus().get("CLOSED")).isEqualTo(0L);
    assertThat(response.totalIncidents()).isEqualTo(3L);
  }

  @Test
  void engineerScopeUsesTheAssigneeQuery() {
    User engineer = new User("engineer@example.com", "hashed", "Engineer", Role.ENGINEER);
    CustomUserDetails principal = new CustomUserDetails(engineer);
    when(incidentRepository.countByStatusForAssignee(principal.getUserId()))
        .thenReturn(List.of(statusCount(IncidentStatus.IN_PROGRESS, 2)));

    var response = new DashboardService(incidentRepository).getMetrics(principal);

    assertThat(response.scope()).isEqualTo("ASSIGNED");
    assertThat(response.countsByStatus().get("IN_PROGRESS")).isEqualTo(2L);
  }

  @Test
  void adminScopeQueriesAcrossAllIncidents() {
    User admin = new User("admin@example.com", "hashed", "Admin", Role.ADMIN);
    CustomUserDetails principal = new CustomUserDetails(admin);
    when(incidentRepository.countByStatus(null))
        .thenReturn(List.of(statusCount(IncidentStatus.RESOLVED, 10)));

    var response = new DashboardService(incidentRepository).getMetrics(principal);

    assertThat(response.scope()).isEqualTo("SYSTEM_WIDE");
    assertThat(response.countsByStatus().get("RESOLVED")).isEqualTo(10L);
  }
}
