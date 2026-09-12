package com.incidentplatform.domain.incident;

/** Matches the {@code chk_incidents_status} CHECK constraint (migration V5). */
public enum IncidentStatus {
  OPEN,
  IN_PROGRESS,
  ESCALATED,
  RESOLVED,
  CLOSED
}
