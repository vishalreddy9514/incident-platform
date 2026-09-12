package com.incidentplatform.incident.dto;

/** {@code reason} is optional — a plain escalation with no stated reason is still valid. */
public record EscalateRequest(String reason) {}
