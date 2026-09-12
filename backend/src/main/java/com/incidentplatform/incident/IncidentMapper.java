package com.incidentplatform.incident;

import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentComment;
import com.incidentplatform.domain.incident.IncidentHistory;
import com.incidentplatform.domain.team.Team;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.incident.dto.CommentResponse;
import com.incidentplatform.incident.dto.HistoryEntryResponse;
import com.incidentplatform.incident.dto.IncidentDetailResponse;
import com.incidentplatform.incident.dto.IncidentSummaryResponse;

/** Manual entity↔DTO mapping, per ADR-0007. */
final class IncidentMapper {

  private IncidentMapper() {}

  static IncidentSummaryResponse toSummary(Incident incident) {
    User assignedTo = incident.getAssignedTo();
    return new IncidentSummaryResponse(
        incident.getId(),
        incident.getTitle(),
        incident.getStatus(),
        incident.getPriority(),
        incident.getSeverity(),
        incident.getCategory().getName(),
        incident.getCreatedBy().getDisplayName(),
        assignedTo != null ? assignedTo.getDisplayName() : null,
        incident.getCreatedAt(),
        incident.getUpdatedAt());
  }

  static IncidentDetailResponse toDetail(Incident incident) {
    User assignedTo = incident.getAssignedTo();
    Team team = incident.getTeam();
    return new IncidentDetailResponse(
        incident.getId(),
        incident.getTitle(),
        incident.getDescription(),
        incident.getStatus(),
        incident.getPriority(),
        incident.getSeverity(),
        incident.getCategory().getId(),
        incident.getCategory().getName(),
        incident.getCreatedBy().getId(),
        incident.getCreatedBy().getDisplayName(),
        assignedTo != null ? assignedTo.getId() : null,
        assignedTo != null ? assignedTo.getDisplayName() : null,
        team != null ? team.getId() : null,
        team != null ? team.getName() : null,
        incident.getCreatedAt(),
        incident.getUpdatedAt());
  }

  static CommentResponse toCommentResponse(IncidentComment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getAuthor().getId(),
        comment.getAuthor().getDisplayName(),
        comment.getBody(),
        comment.getCreatedAt());
  }

  static HistoryEntryResponse toHistoryResponse(IncidentHistory history) {
    return new HistoryEntryResponse(
        history.getId(),
        history.getActor().getId(),
        history.getActor().getDisplayName(),
        history.getFieldChanged(),
        history.getOldValue(),
        history.getNewValue(),
        history.getChangedAt());
  }
}
