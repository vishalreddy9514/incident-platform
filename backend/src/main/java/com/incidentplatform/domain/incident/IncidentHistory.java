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
 * Maps to the {@code incident_history} table (migration V8). Deliberately has no setters beyond
 * construction — the database itself rejects UPDATE/DELETE on this table (see the
 * append-only trigger in V8), and the entity is written to match: nothing in the Java layer
 * offers a way to mutate a persisted row, so the append-only design is reflected in both places,
 * not just enforced silently at the database.
 */
@Entity
@Table(name = "incident_history")
public class IncidentHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id", nullable = false)
  private User actor;

  @Column(name = "field_changed", nullable = false, length = 50)
  private String fieldChanged;

  @Column(name = "old_value", columnDefinition = "TEXT")
  private String oldValue;

  @Column(name = "new_value", columnDefinition = "TEXT")
  private String newValue;

  @CreationTimestamp
  @Column(name = "changed_at", nullable = false, updatable = false)
  private OffsetDateTime changedAt;

  protected IncidentHistory() {
    // required by JPA
  }

  public IncidentHistory(
      Incident incident, User actor, String fieldChanged, String oldValue, String newValue) {
    this.incident = incident;
    this.actor = actor;
    this.fieldChanged = fieldChanged;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public Long getId() {
    return id;
  }

  public Incident getIncident() {
    return incident;
  }

  public User getActor() {
    return actor;
  }

  public String getFieldChanged() {
    return fieldChanged;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }
}
