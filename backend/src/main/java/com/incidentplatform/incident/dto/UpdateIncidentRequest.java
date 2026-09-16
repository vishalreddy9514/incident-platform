package com.incidentplatform.incident.dto;

import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentSeverity;
import com.incidentplatform.domain.incident.IncidentStatus;
import jakarta.validation.constraints.Size;

/**
 * Every field is optional — only non-null fields are applied. Who is allowed to change which fields
 * (and when) is a business rule enforced in {@code IncidentService.update}, not here: a USER may
 * only touch title/description/categoryId, and only while the incident is still OPEN and they're
 * its creator; status/priority/severity changes require ENGINEER or ADMIN.
 */
public record UpdateIncidentRequest(
    @Size(max = 200) String title,
    String description,
    Long categoryId,
    IncidentStatus status,
    IncidentPriority priority,
    IncidentSeverity severity) {}
