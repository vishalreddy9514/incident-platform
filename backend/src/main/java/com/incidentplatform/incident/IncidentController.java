package com.incidentplatform.incident;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.domain.incident.IncidentPriority;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.incident.dto.AssignRequest;
import com.incidentplatform.incident.dto.CommentRequest;
import com.incidentplatform.incident.dto.CommentResponse;
import com.incidentplatform.incident.dto.CreateIncidentRequest;
import com.incidentplatform.incident.dto.EscalateRequest;
import com.incidentplatform.incident.dto.HistoryEntryResponse;
import com.incidentplatform.incident.dto.IncidentDetailResponse;
import com.incidentplatform.incident.dto.IncidentSummaryResponse;
import com.incidentplatform.incident.dto.UpdateIncidentRequest;
import com.incidentplatform.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every method here is reachable by any authenticated user (see SecurityConfig) — the finer-grained
 * rules (who can view/edit *this* incident, who can change status/priority) live in {@link
 * IncidentService}, except for {@link #assign} and {@link #escalate}, which are pure role gates
 * (FR-7, FR-8) applied here via {@code @PreAuthorize}, per ADR-0002.
 */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

  private final IncidentService incidentService;

  public IncidentController(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IncidentDetailResponse create(
      @Valid @RequestBody CreateIncidentRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.create(request, principal);
  }

  @GetMapping
  public PageResponse<IncidentSummaryResponse> list(
      @RequestParam(required = false) IncidentStatus status,
      @RequestParam(required = false) IncidentPriority priority,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long assignedToId,
      @PageableDefault(size = 20) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.list(status, priority, categoryId, assignedToId, pageable, principal);
  }

  @GetMapping("/{id}")
  public IncidentDetailResponse getById(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.getById(id, principal);
  }

  @PutMapping("/{id}")
  public IncidentDetailResponse update(
      @PathVariable Long id,
      @Valid @RequestBody UpdateIncidentRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.update(id, request, principal);
  }

  /** ADMIN-only — no FR explicitly assigns incident deletion to a role; treated as a sensitive
   * administrative action consistent with how deletions are flagged elsewhere (Phase 1 §10). */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
    incidentService.delete(id, principal);
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse addComment(
      @PathVariable Long id,
      @Valid @RequestBody CommentRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.addComment(id, request, principal);
  }

  @GetMapping("/{id}/comments")
  public List<CommentResponse> getComments(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.getComments(id, principal);
  }

  @GetMapping("/{id}/history")
  public List<HistoryEntryResponse> getHistory(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.getHistory(id, principal);
  }

  /** ENGINEER/ADMIN only (FR-8). */
  @PostMapping("/{id}/assign")
  @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
  public IncidentDetailResponse assign(
      @PathVariable Long id,
      @Valid @RequestBody AssignRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.assign(id, request, principal);
  }

  /** ENGINEER/ADMIN only (FR-7 covers status changes generally; escalation specifically per UC-4
   * is an engineer/admin action, not something a reporting USER triggers directly). */
  @PostMapping("/{id}/escalate")
  @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
  public IncidentDetailResponse escalate(
      @PathVariable Long id,
      @Valid @RequestBody EscalateRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return incidentService.escalate(id, request, principal);
  }
}
