package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.IncidentComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

  List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
