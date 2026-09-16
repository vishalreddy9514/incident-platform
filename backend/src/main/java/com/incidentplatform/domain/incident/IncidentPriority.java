package com.incidentplatform.domain.incident;

/**
 * Matches the {@code chk_incidents_priority} CHECK constraint (migration V5). Also reused by {@link
 * com.incidentplatform.domain.ai.AiAnalysis#getPredictedPriority()} since AI-predicted priority and
 * human-set priority are the same value space.
 */
public enum IncidentPriority {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}
