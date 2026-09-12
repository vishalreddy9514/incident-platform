package com.incidentplatform.incident.dto;

import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentSeverity;
import com.incidentplatform.domain.incident.IncidentStatus;
import java.time.OffsetDateTime;

/** What a list/search result row looks like — flattened (no nested objects) to keep it light. */
public record IncidentSummaryResponse(
    Long id,
    String title,
    IncidentStatus status,
    IncidentPriority priority,
    IncidentSeverity severity,
    String categoryName,
    String createdByDisplayName,
    String assignedToDisplayName,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
