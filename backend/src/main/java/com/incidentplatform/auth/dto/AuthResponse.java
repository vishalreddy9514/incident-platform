package com.incidentplatform.auth.dto;

import com.incidentplatform.user.dto.UserResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user) {}
