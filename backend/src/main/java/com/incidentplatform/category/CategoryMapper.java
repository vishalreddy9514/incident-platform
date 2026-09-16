package com.incidentplatform.category;

import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.domain.incident.IncidentCategory;

/**
 * Manual entity↔DTO mapping, per ADR-0007. Deliberately not a Spring bean — a stateless utility.
 */
final class CategoryMapper {

  private CategoryMapper() {}

  static CategoryResponse toResponse(IncidentCategory category) {
    return new CategoryResponse(
        category.getId(), category.getName(), category.getDescription(), category.isActive());
  }
}
