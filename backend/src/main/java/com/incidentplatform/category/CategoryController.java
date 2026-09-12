package com.incidentplatform.category;

import com.incidentplatform.category.dto.CategoryCreateRequest;
import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.category.dto.CategoryUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read access is open to any authenticated user (FR-5 needs a category to exist at incident
 * creation); create/update are ADMIN-only (FR-21). This is the vertical slice proving the full
 * layering works (Phase 4), now extended with the write operations that needed real RBAC to exist
 * first (Phase 5) before they could be safely added.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategoryResponse> getActiveCategories() {
    return categoryService.getActiveCategories();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request) {
    return categoryService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public CategoryResponse update(
      @PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
    return categoryService.update(id, request);
  }
}
