package com.incidentplatform.repository;

import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository
    extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

  /**
   * Aggregate count grouped by status, optionally scoped to one creator (used to implement
   * role-scoped dashboard metrics — see Phase 1 §6.3 UC-7: aggregate queries, not client-side
   * computation over the full dataset). {@code createdById = null} means "all incidents" —
   * used for the ENGINEER/ADMIN system-wide view.
   */
  @Query(
      "SELECT i.status as status, COUNT(i) as count FROM Incident i "
          + "WHERE (:createdById IS NULL OR i.createdBy.id = :createdById) "
          + "GROUP BY i.status")
  List<StatusCount> countByStatus(@Param("createdById") Long createdById);

  /** Same shape as {@link #countByStatus}, scoped by assignee instead of creator — powers the
   * ENGINEER dashboard view (FR-17: assigned load by status). */
  @Query(
      "SELECT i.status as status, COUNT(i) as count FROM Incident i "
          + "WHERE i.assignedTo.id = :assignedToId "
          + "GROUP BY i.status")
  List<StatusCount> countByStatusForAssignee(@Param("assignedToId") Long assignedToId);

  interface StatusCount {
    IncidentStatus getStatus();

    long getCount();
  }
}
