package com.incidentplatform.category;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.incidentplatform.category.dto.CategoryResponse;
import com.incidentplatform.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer slice test: only the MVC infrastructure and {@link CategoryController} are loaded, no
 * database. {@link SecurityConfig} is imported explicitly so this test exercises the same
 * (temporary, Phase 4) permit-all security rule the real application runs under, rather than
 * assuming security is irrelevant to this slice.
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CategoryService categoryService;

  @Test
  void getCategoriesReturnsOkWithJsonBody() throws Exception {
    when(categoryService.getActiveCategories())
        .thenReturn(
            List.of(
                new CategoryResponse(1L, "Hardware", "Physical equipment issues"),
                new CategoryResponse(2L, "Network", "Connectivity issues")));

    mockMvc
        .perform(get("/api/v1/categories").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Hardware"))
        .andExpect(jsonPath("$[1].name").value("Network"));
  }

  @Test
  void getCategoriesReturnsEmptyArrayWhenNoneActive() throws Exception {
    when(categoryService.getActiveCategories()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/categories").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }
}
