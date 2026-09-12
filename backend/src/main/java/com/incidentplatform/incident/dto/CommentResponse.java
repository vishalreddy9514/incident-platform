package com.incidentplatform.incident.dto;

import java.time.OffsetDateTime;

public record CommentResponse(
    Long id, Long authorId, String authorDisplayName, String body, OffsetDateTime createdAt) {}
