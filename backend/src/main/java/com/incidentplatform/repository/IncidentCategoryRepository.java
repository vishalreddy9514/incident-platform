package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.IncidentCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {

  List<IncidentCategory> findByIsActiveTrueOrderByNameAsc();

  /**
   * Matches the DB's {@code uq_incident_categories_name} constraint, which applies regardless of
   * {@code is_active} — a deactivated category's name is still taken.
   */
  boolean existsByNameIgnoreCase(String name);
}
