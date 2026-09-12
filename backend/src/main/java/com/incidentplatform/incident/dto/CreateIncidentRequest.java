package com.incidentplatform.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String description,
    @NotNull Long categoryId) {}
