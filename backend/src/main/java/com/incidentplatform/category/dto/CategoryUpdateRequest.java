package com.incidentplatform.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 2000) String description,
    @NotNull Boolean isActive) {}
