package com.incidentplatform.incident.dto;

import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentSeverity;
import com.incidentplatform.domain.incident.IncidentStatus;
import java.time.OffsetDateTime;

public record IncidentDetailResponse(
    Long id,
    String title,
    String description,
    IncidentStatus status,
    IncidentPriority priority,
    IncidentSeverity severity,
    Long categoryId,
    String categoryName,
    Long createdById,
    String createdByDisplayName,
    Long assignedToId,
    String assignedToDisplayName,
    Long teamId,
    String teamName,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
