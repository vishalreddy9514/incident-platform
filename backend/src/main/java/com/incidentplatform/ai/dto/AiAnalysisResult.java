package com.incidentplatform.ai.dto;

import com.incidentplatform.domain.incident.IncidentPriority;
import java.util.List;

/**
 * The AI service's response body, deserialised directly by Jackson — field names match the
 * service's camelCase JSON exactly (see {@code ai-service/app/schemas.py}), and {@code
 * predictedPriority} maps straight onto {@link IncidentPriority} since both sides deliberately use
 * the same value space (LOW/MEDIUM/HIGH/CRITICAL).
 */
public record AiAnalysisResult(
    String suggestedCategory,
    IncidentPriority predictedPriority,
    String summary,
    List<String> keywords,
    List<String> suggestedSteps,
    String modelUsed) {}
