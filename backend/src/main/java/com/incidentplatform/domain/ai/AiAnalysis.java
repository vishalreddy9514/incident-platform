package com.incidentplatform.domain.ai;

import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentPriority;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps to the {@code ai_analysis} table (migration V10). One incident can have several rows over
 * time (re-analysis after more comments); the most recent row per incident is the "current"
 * analysis. {@code keywords} and {@code suggestedSteps} are JSON arrays serialised as text, using
 * the same Hibernate 6 native JSON mapping as {@link
 * com.incidentplatform.domain.audit.AuditLog#getMetadata()}.
 */
@Entity
@Table(name = "ai_analysis")
public class AiAnalysis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @Column(name = "suggested_category", length = 100)
  private String suggestedCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "predicted_priority", length = 10)
  private IncidentPriority predictedPriority;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String keywords;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "suggested_steps", columnDefinition = "jsonb")
  private String suggestedSteps;

  @Column(name = "model_used", length = 100)
  private String modelUsed;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected AiAnalysis() {
    // required by JPA
  }

  public AiAnalysis(
      Incident incident,
      String suggestedCategory,
      IncidentPriority predictedPriority,
      String summary,
      String keywords,
      String suggestedSteps,
      String modelUsed) {
    this.incident = incident;
    this.suggestedCategory = suggestedCategory;
    this.predictedPriority = predictedPriority;
    this.summary = summary;
    this.keywords = keywords;
    this.suggestedSteps = suggestedSteps;
    this.modelUsed = modelUsed;
  }

  public Long getId() {
    return id;
  }

  public Incident getIncident() {
    return incident;
  }

  public String getSuggestedCategory() {
    return suggestedCategory;
  }

  public IncidentPriority getPredictedPriority() {
    return predictedPriority;
  }

  public String getSummary() {
    return summary;
  }

  public String getKeywords() {
    return keywords;
  }

  public String getSuggestedSteps() {
    return suggestedSteps;
  }

  public String getModelUsed() {
    return modelUsed;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
