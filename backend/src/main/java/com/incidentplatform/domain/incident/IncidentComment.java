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
 * Maps to the {@code incident_comments} table (migration V6). No updated_at — the schema
 * deliberately has no edit-comment column, so comments are immutable once posted (FR-10 doesn't
 * require editing; a future improvement if that changes).
 */
@Entity
@Table(name = "incident_comments")
public class IncidentComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected IncidentComment() {
    // required by JPA
  }

  public IncidentComment(Incident incident, User author, String body) {
    this.incident = incident;
    this.author = author;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public Incident getIncident() {
    return incident;
  }

  public User getAuthor() {
    return author;
  }

  public String getBody() {
    return body;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
