package com.incidentplatform.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incidentplatform.category.dto.CategoryCreateRequest;
import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.category.dto.CategoryUpdateRequest;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.repository.IncidentCategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit test: the repository is mocked, no Spring context or database is involved. This is
 * the fast, isolated layer of the testing pyramid described in Phase 1 §11.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock private IncidentCategoryRepository categoryRepository;

  @Test
  void getActiveCategoriesMapsEntitiesToResponses() {
    IncidentCategory hardware = new IncidentCategory("Hardware", "Physical equipment issues");
    IncidentCategory network = new IncidentCategory("Network", "Connectivity issues");
    when(categoryRepository.findByIsActiveTrueOrderByNameAsc())
        .thenReturn(List.of(hardware, network));

    CategoryService service = new CategoryService(categoryRepository);
    List<CategoryResponse> result = service.getActiveCategories();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("Hardware");
    assertThat(result.get(0).description()).isEqualTo("Physical equipment issues");
    assertThat(result.get(1).name()).isEqualTo("Network");
    verify(categoryRepository).findByIsActiveTrueOrderByNameAsc();
  }

  @Test
  void getActiveCategoriesReturnsEmptyListWhenNoneActive() {
    when(categoryRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());

    CategoryService service = new CategoryService(categoryRepository);

    assertThat(service.getActiveCategories()).isEmpty();
  }

  @Test
  void createSavesANewCategoryWhenNameIsAvailable() {
    when(categoryRepository.existsByNameIgnoreCase("Printers")).thenReturn(false);
    when(categoryRepository.save(any(IncidentCategory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CategoryResponse response =
        new CategoryService(categoryRepository)
            .create(new CategoryCreateRequest("Printers", "Printer issues"));

    assertThat(response.name()).isEqualTo("Printers");
  }

  @Test
  void createRejectsADuplicateNameEvenIfTheExistingCategoryIsDeactivated() {
    when(categoryRepository.existsByNameIgnoreCase("Hardware")).thenReturn(true);

    assertThatThrownBy(
            () ->
                new CategoryService(categoryRepository)
                    .create(new CategoryCreateRequest("Hardware", "Duplicate")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("CATEGORY_NAME_TAKEN");
  }

  @Test
  void updateThrowsWhenCategoryDoesNotExist() {
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new CategoryService(categoryRepository)
                    .update(99L, new CategoryUpdateRequest("New Name", "desc", true)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateAppliesNameDescriptionAndActiveFlag() {
    IncidentCategory category = new IncidentCategory("Old Name", "Old description");
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    CategoryResponse response =
        new CategoryService(categoryRepository)
            .update(1L, new CategoryUpdateRequest("New Name", "New description", false));

    assertThat(response.name()).isEqualTo("New Name");
    assertThat(category.isActive()).isFalse();
  }
}
