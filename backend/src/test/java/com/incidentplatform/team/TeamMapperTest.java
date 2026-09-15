package com.incidentplatform.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.domain.team.Team;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TeamMapperTest {

  @Test
  void toResponseMapsEveryField() {
    Team team = new Team("Platform", "Core platform team");
    ReflectionTestUtils.setField(team, "id", 1L);

    var response = TeamMapper.toResponse(team);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("Platform");
    assertThat(response.description()).isEqualTo("Core platform team");
  }
}
