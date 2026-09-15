package com.incidentplatform.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.domain.team.Team;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserMapperTest {

  @Test
  void toResponseMapsEveryFieldAndOmitsThePasswordHash() {
    User user = new User("user@example.com", "hashed-secret", "A User", Role.ENGINEER);
    ReflectionTestUtils.setField(user, "id", 1L);
    Team team = new Team("Platform", "Core platform team");
    ReflectionTestUtils.setField(team, "id", 2L);
    user.setTeam(team);

    var response = UserMapper.toResponse(user);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.displayName()).isEqualTo("A User");
    assertThat(response.role()).isEqualTo(Role.ENGINEER);
    assertThat(response.teamId()).isEqualTo(2L);
    assertThat(response.isActive()).isTrue();
    assertThat(response.toString()).doesNotContain("hashed-secret");
  }

  @Test
  void toResponseLeavesTeamIdNullWhenUserHasNoTeam() {
    User user = new User("user@example.com", "hashed", "A User", Role.USER);

    var response = UserMapper.toResponse(user);

    assertThat(response.teamId()).isNull();
  }
}
