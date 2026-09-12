package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
  // Filter/search/pagination query methods are added in Phase 6 alongside
  // the incident management endpoints that actually need them (FR-6).
}
