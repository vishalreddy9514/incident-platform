package com.incidentplatform.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.security.CustomUserDetails;
import com.incidentplatform.security.JwtAuthenticationFilter;
import com.incidentplatform.security.RateLimitingFilter;
import com.incidentplatform.user.dto.RoleUpdateRequest;
import com.incidentplatform.user.dto.UserResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC slice test for request validation and response shape — security filters disabled, same
 * rationale as {@code CategoryControllerTest}. RBAC (list is ENGINEER/ADMIN-only, role update is
 * ADMIN-only) is covered end-to-end in {@code AuthenticationIntegrationTest}, not here — see
 * {@code TeamControllerTest}'s javadoc for why a slice test can't exercise {@code @PreAuthorize}.
 * {@code GET /me} needs {@code @AuthenticationPrincipal}, which requires a real security context
 * this slice doesn't build, so it's also left to the integration test.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private UserService userService;
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private RateLimitingFilter rateLimitingFilter;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listUsersReturnsAPagedResult() throws Exception {
    UserResponse user = new UserResponse(1L, "engineer@example.com", "Engineer", Role.ENGINEER, null, true);
    when(userService.listUsers(eq(null), any()))
        .thenReturn(PageResponse.of(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1)));

    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("engineer@example.com"));
  }

  @Test
  void listUsersFiltersByRoleQueryParam() throws Exception {
    when(userService.listUsers(eq(Role.ENGINEER), any()))
        .thenReturn(PageResponse.of(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0)));

    mockMvc.perform(get("/api/v1/users").param("role", "ENGINEER")).andExpect(status().isOk());
  }

  @Test
  void updateRoleReturns400WhenRoleIsMissing() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/users/5/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRoleReturnsTheUpdatedUserGivenAValidRequest() throws Exception {
    UserResponse updated = new UserResponse(5L, "user@example.com", "A User", Role.ENGINEER, null, true);
    when(userService.updateRole(eq(5L), any(RoleUpdateRequest.class), any())).thenReturn(updated);

    // updateRole's @AuthenticationPrincipal CustomUserDetails needs a principal of that exact
    // type in SecurityContextHolder. addFilters = false means no Spring Security filter ever
    // runs, including the one that would normally copy a RequestPostProcessor-supplied
    // Authentication from the (mock) session into SecurityContextHolder — so it's set directly.
    User admin = new User("admin@example.com", "hashed", "Admin", Role.ADMIN);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(new CustomUserDetails(admin), null, List.of()));

    mockMvc
        .perform(
            patch("/api/v1/users/5/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RoleUpdateRequest(Role.ENGINEER))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ENGINEER"));
  }
}
