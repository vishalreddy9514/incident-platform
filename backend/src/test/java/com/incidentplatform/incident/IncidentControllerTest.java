package com.incidentplatform.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentSeverity;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.incident.dto.CreateIncidentRequest;
import com.incidentplatform.incident.dto.IncidentDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC slice test for request validation and response shape — security filters disabled, same
 * rationale as {@code CategoryControllerTest}/{@code AuthControllerTest}: RBAC and ownership
 * rules are covered end-to-end in {@code IncidentManagementIntegrationTest}.
 */
@WebMvcTest(IncidentController.class)
@AutoConfigureMockMvc(addFilters = false)
class IncidentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private IncidentService incidentService;

  @Test
  void createReturns201WithTheCreatedIncident() throws Exception {
    IncidentDetailResponse response =
        new IncidentDetailResponse(
            1L,
            "Laptop won't boot",
            "Black screen",
            IncidentStatus.OPEN,
            IncidentPriority.MEDIUM,
            IncidentSeverity.MEDIUM,
            1L,
            "Hardware",
            1L,
            "Creator",
            null,
            null,
            null,
            null,
            null,
            null);
    when(incidentService.create(any(), any())).thenReturn(response);

    CreateIncidentRequest request = new CreateIncidentRequest("Laptop won't boot", "Black screen", 1L);

    mockMvc
        .perform(
            post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Laptop won't boot"))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void createReturns400WhenTitleIsBlank() throws Exception {
    CreateIncidentRequest request = new CreateIncidentRequest("", "Some description", 1L);

    mockMvc
        .perform(
            post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void createReturns400WhenCategoryIdIsMissing() throws Exception {
    String bodyMissingCategoryId = "{\"title\":\"Title\",\"description\":\"Description\"}";

    mockMvc
        .perform(
            post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyMissingCategoryId))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getByIdReturnsTheIncident() throws Exception {
    IncidentDetailResponse response =
        new IncidentDetailResponse(
            5L,
            "Title",
            "Description",
            IncidentStatus.OPEN,
            IncidentPriority.MEDIUM,
            IncidentSeverity.MEDIUM,
            1L,
            "Hardware",
            1L,
            "Creator",
            null,
            null,
            null,
            null,
            null,
            null);
    when(incidentService.getById(eq(5L), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/incidents/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.categoryName").value("Hardware"));
  }
}
