package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.IncidentCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {

  List<IncidentCategory> findByIsActiveTrueOrderByNameAsc();
}
