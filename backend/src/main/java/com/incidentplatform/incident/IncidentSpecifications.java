package com.incidentplatform.incident;

import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filters for {@code GET /api/v1/incidents}. Specifications are the right tool here
 * rather than a derived-query method per filter combination (FR-6 needs status, priority,
 * category, assignee, and role-based ownership scoping to combine freely) — a
 * {@code findByStatusAndPriorityAndCategory...} method explosion doesn't scale with the number
 * of optional filters, while a single composed {@link Specification} does.
 */
final class IncidentSpecifications {

  private IncidentSpecifications() {}

  static Specification<Incident> withFilters(
      IncidentStatus status,
      IncidentPriority priority,
      Long categoryId,
      Long assignedToId,
      Long createdById) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (priority != null) {
        predicates.add(cb.equal(root.get("priority"), priority));
      }
      if (categoryId != null) {
        predicates.add(cb.equal(root.get("category").get("id"), categoryId));
      }
      if (assignedToId != null) {
        predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
      }
      if (createdById != null) {
        predicates.add(cb.equal(root.get("createdBy").get("id"), createdById));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
