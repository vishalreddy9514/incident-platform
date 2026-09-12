package com.incidentplatform.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.repository.IncidentCategoryRepository;
import java.util.List;
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
}
