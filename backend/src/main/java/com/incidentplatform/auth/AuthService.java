package com.incidentplatform.auth;

import com.incidentplatform.auth.dto.AuthResponse;
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
import com.incidentplatform.user.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately does not use Spring Security's {@code AuthenticationManager}/{@code
 * AuthenticationProvider} machinery — login here is a direct repository lookup plus {@link
 * PasswordEncoder#matches}. See ADR-0008 for why: that machinery exists to support pluggable,
 * often stateful authentication sources, which this stateless single-source (one users table)
 * JWT API doesn't need. {@link com.incidentplatform.security.JwtAuthenticationFilter} handles
 * authenticating subsequent requests from the issued token.
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final AuditLogRepository auditLogRepository;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      AuditLogRepository auditLogRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "EMAIL_ALREADY_REGISTERED",
          "An account with this email already exists");
    }

    User user =
        new User(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.displayName(),
            Role.USER);
    user = userRepository.save(user);
    auditLogRepository.save(new AuditLog(user, "USER_REGISTERED", "User", user.getId(), null));

    return buildAuthResponse(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    // A single generic error for "no such user" and "wrong password" is deliberate — it avoids
    // confirming to an attacker whether a given email is registered at all.
    User user =
        userRepository
            .findByEmail(request.email())
            .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

    if (!user.isActive()) {
      throw new ApiException(
          HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been deactivated");
    }

    auditLogRepository.save(new AuditLog(user, "USER_LOGIN", "User", user.getId(), null));

    return buildAuthResponse(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    Long userId =
        refreshTokenService
            .validateAndRevoke(request.refreshToken())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or has expired"));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or has expired"));

    return buildAuthResponse(user);
  }

  public void logout(RefreshRequest request) {
    refreshTokenService.revoke(request.refreshToken());
  }

  private AuthResponse buildAuthResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    String refreshToken = refreshTokenService.issue(user.getId());
    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        jwtService.getAccessTokenTtlSeconds(),
        UserMapper.toResponse(user));
  }
}
