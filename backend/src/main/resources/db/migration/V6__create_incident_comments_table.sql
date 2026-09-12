-- V6: incident_comments
-- Supports FR-10. ON DELETE CASCADE on incident_id is intentional: comments
-- have no independent meaning once their incident is gone. Deletion of
-- incidents is expected to be rare/admin-only in practice (see incidents
-- table — no soft-delete flag was added here since FR doesn't require
-- recovering deleted incidents; a future improvement if that changes).

CREATE TABLE incident_comments (
    id          BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    author_id   BIGINT NOT NULL REFERENCES users (id),
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_comments_incident_id ON incident_comments (incident_id);
