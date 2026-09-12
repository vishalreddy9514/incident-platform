package com.incidentplatform.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.domain.team.Team;
import com.incidentplatform.repository.TeamRepository;
import com.incidentplatform.team.dto.TeamCreateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;

  @Test
  void listTeamsMapsAllTeams() {
    when(teamRepository.findAll()).thenReturn(List.of(new Team("Platform", "Core platform team")));

    var result = new TeamService(teamRepository).listTeams();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Platform");
  }

  @Test
  void createSavesANewTeamWhenNameIsAvailable() {
    when(teamRepository.existsByNameIgnoreCase("Platform")).thenReturn(false);
    when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

    var response = new TeamService(teamRepository).create(new TeamCreateRequest("Platform", "desc"));

    assertThat(response.name()).isEqualTo("Platform");
  }

  @Test
  void createRejectsADuplicateTeamName() {
    when(teamRepository.existsByNameIgnoreCase("Platform")).thenReturn(true);

    assertThatThrownBy(
            () -> new TeamService(teamRepository).create(new TeamCreateRequest("Platform", "desc")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("TEAM_NAME_TAKEN");
  }
}
