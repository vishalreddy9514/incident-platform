package com.incidentplatform.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.domain.incident.IncidentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryMapperTest {

  @Test
  void toResponseMapsEveryField() {
    IncidentCategory category = new IncidentCategory("Hardware", "Physical equipment issues");
    ReflectionTestUtils.setField(category, "id", 1L);

    var response = CategoryMapper.toResponse(category);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("Hardware");
    assertThat(response.description()).isEqualTo("Physical equipment issues");
    assertThat(response.isActive()).isTrue();
  }

  @Test
  void toResponseReflectsAnInactiveCategory() {
    IncidentCategory category = new IncidentCategory("Legacy", "Retired category");
    category.setActive(false);

    var response = CategoryMapper.toResponse(category);

    assertThat(response.isActive()).isFalse();
  }
}
