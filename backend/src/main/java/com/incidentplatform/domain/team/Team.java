package com.incidentplatform.domain.team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Maps to the {@code teams} table (migration V2). created_at/updated_at are managed by Hibernate
 * for the normal JPA write path; the database also enforces updated_at via a trigger (V1) as a
 * safety net for any writes that bypass the ORM (manual SQL, admin scripts) — see the note on
 * {@link #getUpdatedAt()}.
 */
@Entity
@Table(name = "teams")
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Team() {
    // required by JPA
  }

  public Team(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * The value Hibernate last set. The database trigger from migration V1 is the ultimate source of
   * truth on UPDATE — it overwrites this column with the real commit time regardless of what the
   * ORM sends, so a freshly-fetched entity is always accurate even if this in-memory instance is
   * stale after a save.
   */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
