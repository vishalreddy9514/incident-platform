package com.incidentplatform.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.auth.dto.AuthResponse;
import com.incidentplatform.auth.dto.LoginRequest;
import com.incidentplatform.auth.dto.RegisterRequest;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.security.JwtAuthenticationFilter;
import com.incidentplatform.security.RateLimitingFilter;
import com.incidentplatform.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC slice test for request validation and response shape. Security filters disabled (see {@link
 * com.incidentplatform.category.CategoryControllerTest} for the same rationale) — the fact that
 * these endpoints are genuinely public end-to-end is verified in AuthenticationIntegrationTest
 * against the real SecurityConfig.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthService authService;

  // @WebMvcTest still constructs Filter beans even though addFilters = false means they're never
  // registered in the chain — these are @Component-annotated Filters, so Spring tries to build
  // the real ones, whose constructors need beans (JwtService, StringRedisTemplate) this slice
  // doesn't provide.
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private RateLimitingFilter rateLimitingFilter;

  @Test
  void registerReturns201WithTokensOnValidRequest() throws Exception {
    UserResponse userResponse =
        new UserResponse(1L, "new@example.com", "New User", Role.USER, null, true);
    when(authService.register(any()))
        .thenReturn(
            new AuthResponse("access-token", "refresh-token", "Bearer", 900L, userResponse));

    RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.user.email").value("new@example.com"));
  }

  @Test
  void registerReturns400ForAnInvalidEmail() throws Exception {
    RegisterRequest request = new RegisterRequest("not-an-email", "password123", "New User");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void registerReturns400ForATooShortPassword() throws Exception {
    RegisterRequest request = new RegisterRequest("valid@example.com", "short", "New User");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginReturns200WithTokensOnValidRequest() throws Exception {
    UserResponse userResponse =
        new UserResponse(1L, "user@example.com", "User", Role.USER, null, true);
    when(authService.login(any()))
        .thenReturn(
            new AuthResponse("access-token", "refresh-token", "Bearer", 900L, userResponse));

    LoginRequest request = new LoginRequest("user@example.com", "correct-password");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"));
  }
}
