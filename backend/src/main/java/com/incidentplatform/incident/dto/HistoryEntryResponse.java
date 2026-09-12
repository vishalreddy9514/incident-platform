package com.incidentplatform.incident.dto;

import java.time.OffsetDateTime;

public record HistoryEntryResponse(
    Long id,
    Long actorId,
    String actorDisplayName,
    String fieldChanged,
    String oldValue,
    String newValue,
    OffsetDateTime changedAt) {}
