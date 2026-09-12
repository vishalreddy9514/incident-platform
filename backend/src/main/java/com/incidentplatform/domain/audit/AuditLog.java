package com.incidentplatform.domain.audit;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps to the {@code audit_logs} table (migration V9). Append-only in the same sense as {@link
 * com.incidentplatform.domain.incident.IncidentHistory} — no setters, enforced at the database by
 * a trigger.
 *
 * <p>{@code metadata} is stored as raw JSON text and mapped to the {@code jsonb} column using
 * Hibernate 6's native {@code @JdbcTypeCode(SqlTypes.JSON)} support — no extra mapping library
 * required. Callers pass an already-serialised JSON string (e.g. via Jackson's {@code
 * ObjectMapper}) rather than this entity taking on JSON-serialisation responsibility itself.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 50)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected AuditLog() {
    // required by JPA
  }

  public AuditLog(
      User actor, String action, String entityType, Long entityId, String metadata) {
    this.actor = actor;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.metadata = metadata;
  }

  public Long getId() {
    return id;
  }

  public User getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getMetadata() {
    return metadata;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
