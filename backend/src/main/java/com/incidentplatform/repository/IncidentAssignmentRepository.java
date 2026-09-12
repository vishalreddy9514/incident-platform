package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.IncidentAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAssignmentRepository extends JpaRepository<IncidentAssignment, Long> {

  List<IncidentAssignment> findByIncidentIdOrderByAssignedAtDesc(Long incidentId);
}
