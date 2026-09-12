package com.incidentplatform.category;

import com.incidentplatform.category.dto.CategoryCreateRequest;
import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.category.dto.CategoryUpdateRequest;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.repository.IncidentCategoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for incident categories. Read access (listing active categories) is open to any
 * authenticated user, since a category needs to be visible to pick when creating an incident
 * (FR-5); create/update are ADMIN-only (FR-21), gated at the controller (ADR-0002).
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

  @Transactional
  public CategoryResponse create(CategoryCreateRequest request) {
    if (categoryRepository.existsByNameIgnoreCase(request.name())) {
      throw new ApiException(
          HttpStatus.CONFLICT, "CATEGORY_NAME_TAKEN", "A category with this name already exists");
    }
    IncidentCategory category = new IncidentCategory(request.name(), request.description());
    return CategoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse update(Long id, CategoryUpdateRequest request) {
    IncidentCategory category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("IncidentCategory", id));
    category.setName(request.name());
    category.setDescription(request.description());
    category.setActive(request.isActive());
    return CategoryMapper.toResponse(category);
  }
}
