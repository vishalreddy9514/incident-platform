package com.incidentplatform.dashboard.dto;

import java.util.Map;

/**
 * {@code scope} tells the client what the counts represent — {@code "OWN"} (a USER's own created
 * incidents), {@code "ASSIGNED"} (an ENGINEER's assigned incidents), or {@code "SYSTEM_WIDE"} (an
 * ADMIN's view of every incident) — per FR-16/17/18's role-scoped dashboards. {@code
 * countsByStatus} always contains every {@code IncidentStatus} value as a key, defaulting to 0,
 * so the frontend never has to guard against a missing status when rendering a chart.
 */
public record DashboardMetricsResponse(
    String scope, long totalIncidents, Map<String, Long> countsByStatus) {}
