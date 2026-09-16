package com.incidentplatform.domain.incident;

import com.incidentplatform.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Maps to the {@code incident_assignments} table (migration V7). Records who has owned an incident
 * and when — separate from {@link IncidentHistory}'s general timeline feed (Phase 1 §8.2,
 * ADR-0002). {@code unassignedAt} is set explicitly when the assignment ends (Phase 6), left null
 * while the assignment is current.
 */
@Entity
@Table(name = "incident_assignments")
public class IncidentAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id", nullable = false)
  private User assignedTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_by_id", nullable = false)
  private User assignedBy;

  @CreationTimestamp
  @Column(name = "assigned_at", nullable = false, updatable = false)
  private OffsetDateTime assignedAt;

  @Column(name = "unassigned_at")
  private OffsetDateTime unassignedAt;

  protected IncidentAssignment() {
    // required by JPA
  }

  public IncidentAssignment(Incident incident, User assignedTo, User assignedBy) {
    this.incident = incident;
    this.assignedTo = assignedTo;
    this.assignedBy = assignedBy;
  }

  public Long getId() {
    return id;
  }

  public Incident getIncident() {
    return incident;
  }

  public User getAssignedTo() {
    return assignedTo;
  }

  public User getAssignedBy() {
    return assignedBy;
  }

  public OffsetDateTime getAssignedAt() {
    return assignedAt;
  }

  public OffsetDateTime getUnassignedAt() {
    return unassignedAt;
  }

  public void markUnassigned(OffsetDateTime when) {
    this.unassignedAt = when;
  }
}
