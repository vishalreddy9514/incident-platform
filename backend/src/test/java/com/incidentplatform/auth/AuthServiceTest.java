package com.incidentplatform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incidentplatform.auth.dto.LoginRequest;
import com.incidentplatform.auth.dto.RefreshRequest;
import com.incidentplatform.auth.dto.RegisterRequest;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.domain.audit.AuditLog;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.AuditLogRepository;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.security.JwtService;
import com.incidentplatform.security.RefreshTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private AuditLogRepository auditLogRepository;

  private AuthService authService() {
    return new AuthService(
        userRepository, passwordEncoder, jwtService, refreshTokenService, auditLogRepository);
  }

  @Test
  void registerCreatesUserAndReturnsTokens() {
    RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("access-token");
    when(refreshTokenService.issue(any())).thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

    var response = authService().register(request);

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.user().email()).isEqualTo("new@example.com");
    assertThat(response.user().role()).isEqualTo(Role.USER);
    verify(auditLogRepository).save(any(AuditLog.class));
  }

  @Test
  void registerRejectsADuplicateEmail() {
    RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Someone");
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService().register(request))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.CONFLICT);

    verify(userRepository, never()).save(any());
  }

  @Test
  void loginSucceedsWithCorrectCredentials() {
    User user = new User("user@example.com", "hashed", "User", Role.USER);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
    when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("access-token");
    when(refreshTokenService.issue(any())).thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

    var response = authService().login(new LoginRequest("user@example.com", "correct-password"));

    assertThat(response.accessToken()).isEqualTo("access-token");
    verify(auditLogRepository).save(any(AuditLog.class));
  }

  @Test
  void loginRejectsAnIncorrectPasswordWithoutRevealingWhichPartWasWrong() {
    User user = new User("user@example.com", "hashed", "User", Role.USER);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

    assertThatThrownBy(
            () -> authService().login(new LoginRequest("user@example.com", "wrong-password")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void loginRejectsAnUnknownEmailWithTheSameGenericError() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService().login(new LoginRequest("nobody@example.com", "whatever")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void loginRejectsADeactivatedAccount() {
    User user = new User("inactive@example.com", "hashed", "Inactive User", Role.USER);
    user.setActive(false);
    when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

    assertThatThrownBy(
            () -> authService().login(new LoginRequest("inactive@example.com", "password")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("ACCOUNT_DISABLED");
  }

  @Test
  void refreshIssuesNewTokensForAValidRefreshToken() {
    User user = new User("user@example.com", "hashed", "User", Role.USER);
    when(refreshTokenService.validateAndRevoke("valid-refresh-token")).thenReturn(Optional.of(7L));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(jwtService.generateAccessToken(any(), anyString(), any())).thenReturn("new-access-token");
    when(refreshTokenService.issue(any())).thenReturn("new-refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

    var response = authService().refresh(new RefreshRequest("valid-refresh-token"));

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
  }

  @Test
  void refreshRejectsAnInvalidOrAlreadyUsedToken() {
    when(refreshTokenService.validateAndRevoke("bad-token")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService().refresh(new RefreshRequest("bad-token")))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("INVALID_REFRESH_TOKEN");
  }

  @Test
  void logoutRevokesTheRefreshToken() {
    authService().logout(new RefreshRequest("some-token"));

    verify(refreshTokenService).revoke("some-token");
  }
}
