package com.incidentplatform.user.dto;

import com.incidentplatform.domain.user.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {}
