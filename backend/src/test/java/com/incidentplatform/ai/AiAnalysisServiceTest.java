package com.incidentplatform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incidentplatform.ai.dto.AiAnalysisResult;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.ai.AiAnalysis;
import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.incident.IncidentService;
import com.incidentplatform.repository.AiAnalysisRepository;
import com.incidentplatform.security.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

  @Mock private AiAnalysisClient client;
  @Mock private AiAnalysisRepository aiAnalysisRepository;
  @Mock private IncidentService incidentService;

  private AiAnalysisService service;
  private Incident incident;
  private CustomUserDetails principal;

  @BeforeEach
  void setUp() {
    service = new AiAnalysisService(client, aiAnalysisRepository, incidentService);
    IncidentCategory category = new IncidentCategory("Network", "Connectivity issues");
    User creator = new User("user@example.com", "hashed", "Creator", Role.USER);
    incident = new Incident("VPN down", "Cannot connect to VPN", category, creator);
    principal = new CustomUserDetails(creator);
  }

  @Test
  void analyseCallsTheClientAndPersistsTheResult() {
    when(incidentService.requireViewableIncident(10L, principal)).thenReturn(incident);
    AiAnalysisResult result =
        new AiAnalysisResult(
            "Network",
            IncidentPriority.HIGH,
            "User cannot connect to the VPN.",
            List.of("vpn", "connection"),
            List.of("Restart VPN client"),
            "mock-heuristic-v1");
    when(client.analyse(any())).thenReturn(result);
    when(aiAnalysisRepository.save(any(AiAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

    var response = service.analyse(10L, principal);

    assertThat(response.suggestedCategory()).isEqualTo("Network");
    assertThat(response.predictedPriority()).isEqualTo(IncidentPriority.HIGH);
    assertThat(response.keywords()).containsExactly("vpn", "connection");
    assertThat(response.suggestedSteps()).containsExactly("Restart VPN client");
    assertThat(response.modelUsed()).isEqualTo("mock-heuristic-v1");
  }

  @Test
  void getLatestReturnsTheMostRecentAnalysis() {
    when(incidentService.requireViewableIncident(10L, principal)).thenReturn(incident);
    AiAnalysis analysis =
        new AiAnalysis(
            incident,
            "Network",
            IncidentPriority.HIGH,
            "Summary",
            AiAnalysisMapper.toJson(List.of("vpn")),
            AiAnalysisMapper.toJson(List.of("step one")),
            "mock-heuristic-v1");
    when(aiAnalysisRepository.findByIncidentIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(analysis));

    var response = service.getLatest(10L, principal);

    assertThat(response.suggestedCategory()).isEqualTo("Network");
    assertThat(response.keywords()).containsExactly("vpn");
  }

  @Test
  void getLatestThrowsWhenNoAnalysisExistsYet() {
    when(incidentService.requireViewableIncident(10L, principal)).thenReturn(incident);
    when(aiAnalysisRepository.findByIncidentIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

    assertThatThrownBy(() -> service.getLatest(10L, principal))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
