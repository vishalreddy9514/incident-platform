package com.incidentplatform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentplatform.ai.dto.AiAnalysisResponse;
import com.incidentplatform.domain.ai.AiAnalysis;
import java.util.List;

/**
 * Manual entity/DTO mapping (ADR-0007), plus the {@code keywords}/{@code suggestedSteps} jsonb
 * columns' text <-> {@code List<String>} conversion. Unlike {@code AuditLog}'s single-field
 * metadata (built by hand with {@code String.formatted}), these arrays hold arbitrary text from an
 * external service, so correct JSON escaping is worth a real JSON library rather than string
 * concatenation.
 */
final class AiAnalysisMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private AiAnalysisMapper() {}

  static String toJson(List<String> values) {
    try {
      return OBJECT_MAPPER.writeValueAsString(values);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialise a String list to JSON", ex);
    }
  }

  static List<String> fromJson(String json) {
    try {
      return OBJECT_MAPPER.readValue(json, STRING_LIST);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to parse a stored JSON string list: " + json, ex);
    }
  }

  static AiAnalysisResponse toResponse(AiAnalysis analysis) {
    return new AiAnalysisResponse(
        analysis.getId(),
        analysis.getIncident().getId(),
        analysis.getSuggestedCategory(),
        analysis.getPredictedPriority(),
        analysis.getSummary(),
        fromJson(analysis.getKeywords()),
        fromJson(analysis.getSuggestedSteps()),
        analysis.getModelUsed(),
        analysis.getCreatedAt());
  }
}
