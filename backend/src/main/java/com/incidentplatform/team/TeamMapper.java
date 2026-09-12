package com.incidentplatform.team;

import com.incidentplatform.domain.team.Team;
import com.incidentplatform.team.dto.TeamResponse;

/** Manual entity↔DTO mapping, per ADR-0007. */
final class TeamMapper {

  private TeamMapper() {}

  static TeamResponse toResponse(Team team) {
    return new TeamResponse(team.getId(), team.getName(), team.getDescription());
  }
}
