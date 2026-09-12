package com.incidentplatform.repository;

import com.incidentplatform.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

  /** Matches the DB's {@code uq_teams_name} constraint. */
  boolean existsByNameIgnoreCase(String name);
}
