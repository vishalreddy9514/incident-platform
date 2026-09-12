package com.incidentplatform.category;

import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.repository.IncidentCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service layer for incident categories. Currently read-only (listing active categories, per
 * FR-5 needing a category to exist at incident creation); create/update/deactivate are added in
 * Phase 6 alongside the ADMIN-only category management endpoints (FR-21), once real RBAC exists
 * to guard them — adding unguarded write endpoints now would be a genuine security gap, not just
 * an incomplete feature.
 */
@Service
public class CategoryService {

  private final IncidentCategoryRepository categoryRepository;

  public CategoryService(IncidentCategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  public List<CategoryResponse> getActiveCategories() {
    return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
        .map(CategoryMapper::toResponse)
        .toList();
  }
}
