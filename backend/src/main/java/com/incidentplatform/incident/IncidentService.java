package com.incidentplatform.incident;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.audit.AuditLog;
import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentAssignment;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.domain.incident.IncidentComment;
import com.incidentplatform.domain.incident.IncidentHistory;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.incident.dto.AssignRequest;
import com.incidentplatform.incident.dto.CommentRequest;
import com.incidentplatform.incident.dto.CommentResponse;
import com.incidentplatform.incident.dto.CreateIncidentRequest;
import com.incidentplatform.incident.dto.EscalateRequest;
import com.incidentplatform.incident.dto.HistoryEntryResponse;
import com.incidentplatform.incident.dto.IncidentDetailResponse;
import com.incidentplatform.incident.dto.IncidentSummaryResponse;
import com.incidentplatform.incident.dto.UpdateIncidentRequest;
import com.incidentplatform.repository.AuditLogRepository;
import com.incidentplatform.repository.IncidentAssignmentRepository;
import com.incidentplatform.repository.IncidentCategoryRepository;
import com.incidentplatform.repository.IncidentCommentRepository;
import com.incidentplatform.repository.IncidentHistoryRepository;
import com.incidentplatform.repository.IncidentRepository;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.security.CustomUserDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core incident workflow (FR-5 to FR-12). Role gates (who can call which action at all) are
 * applied at the controller via {@code @PreAuthorize} where the rule is a pure role check;
 * ownership and business-state rules (who can view/edit *this specific* incident, and when) live
 * here, per ADR-0002 — the two kinds of rule don't fit the same enforcement point.
 */
@Service
public class IncidentService {

  /**
   * Valid status transitions. CLOSED is terminal by design — reopening a closed incident is a
   * deliberate future-improvement candidate (e.g. a dedicated "reopen" action with its own audit
   * trail), not something this map silently allows.
   */
  private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          IncidentStatus.OPEN,
              Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.ESCALATED, IncidentStatus.CLOSED),
          IncidentStatus.IN_PROGRESS,
              Set.of(IncidentStatus.ESCALATED, IncidentStatus.RESOLVED, IncidentStatus.OPEN),
          IncidentStatus.ESCALATED,
              Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED),
          IncidentStatus.RESOLVED,
              Set.of(IncidentStatus.CLOSED, IncidentStatus.IN_PROGRESS),
          IncidentStatus.CLOSED, Set.of());

  private final IncidentRepository incidentRepository;
  private final IncidentCategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final IncidentCommentRepository commentRepository;
  private final IncidentAssignmentRepository assignmentRepository;
  private final IncidentHistoryRepository historyRepository;
  private final AuditLogRepository auditLogRepository;

  public IncidentService(
      IncidentRepository incidentRepository,
      IncidentCategoryRepository categoryRepository,
      UserRepository userRepository,
      IncidentCommentRepository commentRepository,
      IncidentAssignmentRepository assignmentRepository,
      IncidentHistoryRepository historyRepository,
      AuditLogRepository auditLogRepository) {
    this.incidentRepository = incidentRepository;
    this.categoryRepository = categoryRepository;
    this.userRepository = userRepository;
    this.commentRepository = commentRepository;
    this.assignmentRepository = assignmentRepository;
    this.historyRepository = historyRepository;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public IncidentDetailResponse create(CreateIncidentRequest request, CustomUserDetails principal) {
    IncidentCategory category = requireCategory(request.categoryId());
    User creator = requireUser(principal.getUserId());

    Incident incident = new Incident(request.title(), request.description(), category, creator);
    incident = incidentRepository.save(incident);
    recordHistory(incident, creator, "status", null, incident.getStatus().name());

    return IncidentMapper.toDetail(incident);
  }

  @Transactional(readOnly = true)
  public PageResponse<IncidentSummaryResponse> list(
      IncidentStatus status,
      IncidentPriority priority,
      Long categoryId,
      Long assignedToId,
      Pageable pageable,
      CustomUserDetails principal) {
    Long scopedCreatedById =
        principal.getUser().getRole() == Role.USER ? principal.getUserId() : null;
    var spec =
        IncidentSpecifications.withFilters(
            status, priority, categoryId, assignedToId, scopedCreatedById);
    Page<Incident> page = incidentRepository.findAll(spec, pageable);
    return PageResponse.of(page.map(IncidentMapper::toSummary));
  }

  @Transactional(readOnly = true)
  public IncidentDetailResponse getById(Long id, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    assertCanView(incident, principal);
    return IncidentMapper.toDetail(incident);
  }

  @Transactional
  public IncidentDetailResponse update(
      Long id, UpdateIncidentRequest request, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    boolean isPrivileged = principal.getUser().getRole() != Role.USER;
    boolean isOwner = incident.getCreatedBy().getId().equals(principal.getUserId());

    if (!isPrivileged) {
      if (!isOwner) {
        throw new AccessDeniedException("You can only edit your own incidents");
      }
      if (incident.getStatus() != IncidentStatus.OPEN) {
        throw new AccessDeniedException("This incident can no longer be edited by its creator");
      }
      if (request.status() != null || request.priority() != null || request.severity() != null) {
        throw new AccessDeniedException(
            "Only engineers or admins can change status, priority, or severity");
      }
    }

    User actor = requireUser(principal.getUserId());

    if (request.title() != null
        && !request.title().isBlank()
        && !request.title().equals(incident.getTitle())) {
      recordHistory(incident, actor, "title", incident.getTitle(), request.title());
      incident.setTitle(request.title());
    }

    if (request.description() != null
        && !request.description().isBlank()
        && !request.description().equals(incident.getDescription())) {
      recordHistory(incident, actor, "description", incident.getDescription(), request.description());
      incident.setDescription(request.description());
    }

    if (request.categoryId() != null
        && !request.categoryId().equals(incident.getCategory().getId())) {
      IncidentCategory category = requireCategory(request.categoryId());
      recordHistory(incident, actor, "category", incident.getCategory().getName(), category.getName());
      incident.setCategory(category);
    }

    if (request.priority() != null && request.priority() != incident.getPriority()) {
      recordHistory(incident, actor, "priority", incident.getPriority().name(), request.priority().name());
      incident.setPriority(request.priority());
    }

    if (request.severity() != null && request.severity() != incident.getSeverity()) {
      recordHistory(incident, actor, "severity", incident.getSeverity().name(), request.severity().name());
      incident.setSeverity(request.severity());
    }

    if (request.status() != null && request.status() != incident.getStatus()) {
      validateTransition(incident.getStatus(), request.status());
      recordHistory(incident, actor, "status", incident.getStatus().name(), request.status().name());
      incident.setStatus(request.status());
    }

    return IncidentMapper.toDetail(incident);
  }

  @Transactional
  public void delete(Long id, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    User actor = requireUser(principal.getUserId());
    String metadata = "{\"title\":\"%s\"}".formatted(incident.getTitle().replace("\"", "\\\""));
    auditLogRepository.save(new AuditLog(actor, "INCIDENT_DELETED", "Incident", id, metadata));
    incidentRepository.delete(incident);
  }

  @Transactional
  public CommentResponse addComment(Long id, CommentRequest request, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    assertCanView(incident, principal);
    User author = requireUser(principal.getUserId());
    IncidentComment comment = commentRepository.save(new IncidentComment(incident, author, request.body()));
    return IncidentMapper.toCommentResponse(comment);
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> getComments(Long id, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    assertCanView(incident, principal);
    return commentRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
        .map(IncidentMapper::toCommentResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<HistoryEntryResponse> getHistory(Long id, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    assertCanView(incident, principal);
    return historyRepository.findByIncidentIdOrderByChangedAtAsc(id).stream()
        .map(IncidentMapper::toHistoryResponse)
        .toList();
  }

  @Transactional
  public IncidentDetailResponse assign(Long id, AssignRequest request, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    User assignee = requireUser(request.assignedToUserId());
    if (assignee.getRole() == Role.USER) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "INVALID_ASSIGNEE",
          "Incidents can only be assigned to engineers or admins");
    }
    User actor = requireUser(principal.getUserId());

    assignmentRepository.findByIncidentIdOrderByAssignedAtDesc(id).stream()
        .filter(a -> a.getUnassignedAt() == null)
        .findFirst()
        .ifPresent(a -> a.markUnassigned(OffsetDateTime.now()));

    assignmentRepository.save(new IncidentAssignment(incident, assignee, actor));

    String previousAssignee =
        incident.getAssignedTo() != null ? incident.getAssignedTo().getDisplayName() : "Unassigned";
    recordHistory(incident, actor, "assigned_to", previousAssignee, assignee.getDisplayName());
    incident.setAssignedTo(assignee);

    // Assignment implies triage has started - a reasonable default, not a hard requirement, so
    // it only fires from OPEN (an already in-progress/escalated incident being reassigned keeps
    // its current status).
    if (incident.getStatus() == IncidentStatus.OPEN) {
      recordHistory(incident, actor, "status", incident.getStatus().name(), IncidentStatus.IN_PROGRESS.name());
      incident.setStatus(IncidentStatus.IN_PROGRESS);
    }

    return IncidentMapper.toDetail(incident);
  }

  @Transactional
  public IncidentDetailResponse escalate(Long id, EscalateRequest request, CustomUserDetails principal) {
    Incident incident = requireIncident(id);
    validateTransition(incident.getStatus(), IncidentStatus.ESCALATED);
    User actor = requireUser(principal.getUserId());

    String newValue =
        request.reason() != null && !request.reason().isBlank()
            ? "ESCALATED (%s)".formatted(request.reason())
            : "ESCALATED";
    recordHistory(incident, actor, "status", incident.getStatus().name(), newValue);
    incident.setStatus(IncidentStatus.ESCALATED);

    return IncidentMapper.toDetail(incident);
  }

  private void assertCanView(Incident incident, CustomUserDetails principal) {
    boolean isOwner = incident.getCreatedBy().getId().equals(principal.getUserId());
    if (principal.getUser().getRole() == Role.USER && !isOwner) {
      throw new AccessDeniedException("You can only view your own incidents");
    }
  }

  private void validateTransition(IncidentStatus from, IncidentStatus to) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "INVALID_STATUS_TRANSITION",
          "Cannot transition an incident from %s to %s".formatted(from, to));
    }
  }

  private void recordHistory(Incident incident, User actor, String field, String oldValue, String newValue) {
    historyRepository.save(new IncidentHistory(incident, actor, field, oldValue, newValue));
  }

  private Incident requireIncident(Long id) {
    return incidentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident", id));
  }

  private IncidentCategory requireCategory(Long id) {
    return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("IncidentCategory", id));
  }

  private User requireUser(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }
}
