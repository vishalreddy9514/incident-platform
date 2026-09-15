package com.incidentplatform.team;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.security.JwtAuthenticationFilter;
import com.incidentplatform.security.RateLimitingFilter;
import com.incidentplatform.team.dto.TeamCreateRequest;
import com.incidentplatform.team.dto.TeamResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC slice test for request validation and response shape — security filters disabled, same
 * rationale as {@code CategoryControllerTest}/{@code AuthControllerTest}. {@code @PreAuthorize}
 * does not actually apply in this reduced context either (confirmed empirically — a role that
 * should be rejected by {@code hasRole('ADMIN')} still reaches {@link TeamController#create}), so
 * RBAC denial for {@code POST /api/v1/teams} is covered end-to-end in {@code
 * AuthenticationIntegrationTest} instead, not here.
 */
@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private TeamService teamService;
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private RateLimitingFilter rateLimitingFilter;

  @Test
  void listTeamsReturnsOkWithJsonBody() throws Exception {
    when(teamService.listTeams())
        .thenReturn(List.of(new TeamResponse(1L, "Platform", "Core platform team")));

    mockMvc
        .perform(get("/api/v1/teams"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Platform"));
  }

  @Test
  void createReturns201WithTheCreatedTeam() throws Exception {
    when(teamService.create(any()))
        .thenReturn(new TeamResponse(1L, "Platform", "Core platform team"));
    TeamCreateRequest request = new TeamCreateRequest("Platform", "Core platform team");

    mockMvc
        .perform(
            post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Platform"));
  }

  @Test
  void createReturns400WhenNameIsBlank() throws Exception {
    TeamCreateRequest request = new TeamCreateRequest("", "Core platform team");

    mockMvc
        .perform(
            post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
