-- V5: incidents
--
-- The central entity. category_id is NOT NULL (FR-5 requires a category at
-- creation); assigned_to_id and team_id are nullable because an incident
-- can exist unassigned (UC-2, before triage in UC-3).
--
-- Indexes on status, assigned_to_id, category_id and created_at directly
-- support the search/filter/sort requirements in FR-6 and the NFR
-- performance target (<500ms list/search at ~10k rows).

CREATE TABLE incidents (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category_id     BIGINT NOT NULL REFERENCES incident_categories (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    severity        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    created_by_id   BIGINT NOT NULL REFERENCES users (id),
    assigned_to_id  BIGINT NULL REFERENCES users (id) ON DELETE SET NULL,
    team_id         BIGINT NULL REFERENCES teams (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_incidents_status CHECK (
        status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT chk_incidents_priority CHECK (
        priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    CONSTRAINT chk_incidents_severity CHECK (
        severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    )
);

CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_assigned_to_id ON incidents (assigned_to_id);
CREATE INDEX idx_incidents_category_id ON incidents (category_id);
CREATE INDEX idx_incidents_created_at ON incidents (created_at);
CREATE INDEX idx_incidents_created_by_id ON incidents (created_by_id);
CREATE INDEX idx_incidents_team_id ON incidents (team_id);

CREATE TRIGGER trg_incidents_set_updated_at
    BEFORE UPDATE ON incidents
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
