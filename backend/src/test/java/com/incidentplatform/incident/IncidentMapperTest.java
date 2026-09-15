package com.incidentplatform.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.domain.incident.IncidentComment;
import com.incidentplatform.domain.incident.IncidentHistory;
import com.incidentplatform.domain.team.Team;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IncidentMapperTest {

  private final IncidentCategory category = new IncidentCategory("Hardware", "Physical issues");
  private final User creator = new User("creator@example.com", "hashed", "Creator", Role.USER);

  @Test
  void toSummaryMapsFlattenedFieldsWithNoAssignee() {
    Incident incident = new Incident("Title", "Description", category, creator);

    var summary = IncidentMapper.toSummary(incident);

    assertThat(summary.title()).isEqualTo("Title");
    assertThat(summary.categoryName()).isEqualTo("Hardware");
    assertThat(summary.createdByDisplayName()).isEqualTo("Creator");
    assertThat(summary.assignedToDisplayName()).isNull();
  }

  @Test
  void toDetailIncludesAssigneeAndTeamWhenPresent() {
    Incident incident = new Incident("Title", "Description", category, creator);
    User engineer = new User("engineer@example.com", "hashed", "Engineer", Role.ENGINEER);
    ReflectionTestUtils.setField(engineer, "id", 2L);
    incident.setAssignedTo(engineer);
    Team team = new Team("Platform", "Core platform team");
    ReflectionTestUtils.setField(team, "id", 3L);
    incident.setTeam(team);

    var detail = IncidentMapper.toDetail(incident);

    assertThat(detail.assignedToId()).isEqualTo(2L);
    assertThat(detail.assignedToDisplayName()).isEqualTo("Engineer");
    assertThat(detail.teamId()).isEqualTo(3L);
    assertThat(detail.teamName()).isEqualTo("Platform");
  }

  @Test
  void toDetailLeavesAssigneeAndTeamNullWhenAbsent() {
    Incident incident = new Incident("Title", "Description", category, creator);

    var detail = IncidentMapper.toDetail(incident);

    assertThat(detail.assignedToId()).isNull();
    assertThat(detail.assignedToDisplayName()).isNull();
    assertThat(detail.teamId()).isNull();
    assertThat(detail.teamName()).isNull();
  }

  @Test
  void toCommentResponseMapsAuthorAndBody() {
    Incident incident = new Incident("Title", "Description", category, creator);
    IncidentComment comment = new IncidentComment(incident, creator, "Investigating now");

    var response = IncidentMapper.toCommentResponse(comment);

    assertThat(response.authorDisplayName()).isEqualTo("Creator");
    assertThat(response.body()).isEqualTo("Investigating now");
  }

  @Test
  void toHistoryResponseMapsFieldChangeAndActor() {
    Incident incident = new Incident("Title", "Description", category, creator);
    IncidentHistory history = new IncidentHistory(incident, creator, "status", "OPEN", "CLOSED");

    var response = IncidentMapper.toHistoryResponse(history);

    assertThat(response.actorDisplayName()).isEqualTo("Creator");
    assertThat(response.fieldChanged()).isEqualTo("status");
    assertThat(response.oldValue()).isEqualTo("OPEN");
    assertThat(response.newValue()).isEqualTo("CLOSED");
  }
}
