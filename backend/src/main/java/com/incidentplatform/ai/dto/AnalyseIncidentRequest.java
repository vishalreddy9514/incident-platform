package com.incidentplatform.ai.dto;

/** Outbound request body to the AI service's {@code POST /internal/v1/analyse} (FR-13). */
public record AnalyseIncidentRequest(String title, String description, String category) {}
