package com.incidentplatform.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {}
