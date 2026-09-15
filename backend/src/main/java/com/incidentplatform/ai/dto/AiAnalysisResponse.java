package com.incidentplatform.ai.dto;

import com.incidentplatform.domain.incident.IncidentPriority;
import java.time.OffsetDateTime;
import java.util.List;

/** The persisted {@code AiAnalysis} record, shaped for the frontend (FR-13/FR-14). */
public record AiAnalysisResponse(
    Long id,
    Long incidentId,
    String suggestedCategory,
    IncidentPriority predictedPriority,
    String summary,
    List<String> keywords,
    List<String> suggestedSteps,
    String modelUsed,
    OffsetDateTime createdAt) {}
