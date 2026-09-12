package com.incidentplatform.incident.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull Long assignedToUserId) {}
