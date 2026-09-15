package com.incidentplatform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incidentplatform.domain.ai.AiAnalysis;
import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiAnalysisMapperTest {

  @Test
  void toJsonAndFromJsonRoundTripAListOfStrings() {
    List<String> original = List.of("vpn", "connection", "timeout");

    String json = AiAnalysisMapper.toJson(original);

    assertThat(json).isEqualTo("[\"vpn\",\"connection\",\"timeout\"]");
    assertThat(AiAnalysisMapper.fromJson(json)).containsExactlyElementsOf(original);
  }

  @Test
  void toJsonEscapesQuotesInsideValues() {
    List<String> values = List.of("say \"hello\"");

    String json = AiAnalysisMapper.toJson(values);

    assertThat(AiAnalysisMapper.fromJson(json)).containsExactly("say \"hello\"");
  }

  @Test
  void fromJsonRejectsMalformedInput() {
    assertThatThrownBy(() -> AiAnalysisMapper.fromJson("not json"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void toResponseMapsTheStoredAnalysis() {
    IncidentCategory category = new IncidentCategory("Network", "Connectivity issues");
    User creator = new User("user@example.com", "hashed", "User", Role.USER);
    Incident incident = new Incident("VPN down", "Cannot connect", category, creator);
    ReflectionTestUtils.setField(incident, "id", 7L);
    AiAnalysis analysis =
        new AiAnalysis(
            incident,
            "Network",
            IncidentPriority.HIGH,
            "Summary text",
            AiAnalysisMapper.toJson(List.of("vpn")),
            AiAnalysisMapper.toJson(List.of("Restart VPN client")),
            "mock-heuristic-v1");

    var response = AiAnalysisMapper.toResponse(analysis);

    assertThat(response.incidentId()).isEqualTo(7L);
    assertThat(response.suggestedCategory()).isEqualTo("Network");
    assertThat(response.predictedPriority()).isEqualTo(IncidentPriority.HIGH);
    assertThat(response.keywords()).containsExactly("vpn");
    assertThat(response.suggestedSteps()).containsExactly("Restart VPN client");
    assertThat(response.modelUsed()).isEqualTo("mock-heuristic-v1");
  }
}
