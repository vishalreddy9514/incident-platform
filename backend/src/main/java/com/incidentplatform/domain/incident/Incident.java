package com.incidentplatform.domain.incident;

import com.incidentplatform.domain.team.Team;
import com.incidentplatform.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Maps to the {@code incidents} table (migration V5) — the central entity. Deliberately
 * unidirectional (no {@code @OneToMany} back-references to comments/history/assignments): those are
 * queried explicitly through their own repositories, keeping this entity simple and avoiding
 * accidental N+1 loading. See Phase 1 §8.2 and ADR-0002 for why assignment history and the general
 * timeline are separate concepts, not folded into this entity.
 */
@Entity
@Table(name = "incidents")
public class Incident {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private IncidentCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IncidentStatus status = IncidentStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private IncidentPriority priority = IncidentPriority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private IncidentSeverity severity = IncidentSeverity.MEDIUM;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id")
  private User assignedTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Incident() {
    // required by JPA
  }

  public Incident(String title, String description, IncidentCategory category, User createdBy) {
    this.title = title;
    this.description = description;
    this.category = category;
    this.createdBy = createdBy;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IncidentCategory getCategory() {
    return category;
  }

  public void setCategory(IncidentCategory category) {
    this.category = category;
  }

  public IncidentStatus getStatus() {
    return status;
  }

  public void setStatus(IncidentStatus status) {
    this.status = status;
  }

  public IncidentPriority getPriority() {
    return priority;
  }

  public void setPriority(IncidentPriority priority) {
    this.priority = priority;
  }

  public IncidentSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(IncidentSeverity severity) {
    this.severity = severity;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public User getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(User assignedTo) {
    this.assignedTo = assignedTo;
  }

  public Team getTeam() {
    return team;
  }

  public void setTeam(Team team) {
    this.team = team;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
