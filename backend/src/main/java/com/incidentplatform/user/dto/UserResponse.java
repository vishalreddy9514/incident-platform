package com.incidentplatform.user.dto;

import com.incidentplatform.domain.user.Role;

/** What the API returns for a user — never the entity, never the password hash. */
public record UserResponse(
    Long id, String email, String displayName, Role role, Long teamId, boolean isActive) {}
