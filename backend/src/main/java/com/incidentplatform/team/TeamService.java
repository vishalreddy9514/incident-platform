package com.incidentplatform.team;

import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.domain.team.Team;
import com.incidentplatform.repository.TeamRepository;
import com.incidentplatform.team.dto.TeamCreateRequest;
import com.incidentplatform.team.dto.TeamResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access open to any authenticated user (needed to pick a team when assigning/creating
 * incidents); create is ADMIN-only (FR-19 "manage teams"), gated at the controller.
 */
@Service
public class TeamService {

  private final TeamRepository teamRepository;

  public TeamService(TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
  }

  public List<TeamResponse> listTeams() {
    return teamRepository.findAll().stream().map(TeamMapper::toResponse).toList();
  }

  @Transactional
  public TeamResponse create(TeamCreateRequest request) {
    if (teamRepository.existsByNameIgnoreCase(request.name())) {
      throw new ApiException(
          HttpStatus.CONFLICT, "TEAM_NAME_TAKEN", "A team with this name already exists");
    }
    Team team = new Team(request.name(), request.description());
    return TeamMapper.toResponse(teamRepository.save(team));
  }
}
