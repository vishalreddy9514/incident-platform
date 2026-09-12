package com.incidentplatform.category;

import com.incidentplatform.category.dto.CategoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only category listing, versioned under /api/v1 per Phase 1 §9.1. This is the vertical
 * slice proving the full Controller → Service → Domain → Repository → Database path works before
 * Phase 5/6 build the larger, auth-guarded incident management surface on the same pattern.
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
}
