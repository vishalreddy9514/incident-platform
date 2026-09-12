package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.IncidentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

  // Insert-and-read only, by design (see IncidentHistory and migration V8's
  // append-only trigger) — this repository is never used for update/delete.
  List<IncidentHistory> findByIncidentIdOrderByChangedAtAsc(Long incidentId);
}
