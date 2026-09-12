-- V7: incident_assignments
--
-- Deliberately separate from incidents.assigned_to_id (the current owner)
-- and from incident_history (the general timeline feed) — see Phase 1 §8.2
-- and ADR-0002. This table answers "who has owned this incident and when",
-- which incident_history's generic field_changed/old_value/new_value shape
-- would make awkward to query directly.

CREATE TABLE incident_assignments (
    id             BIGSERIAL PRIMARY KEY,
    incident_id    BIGINT NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    assigned_to_id BIGINT NOT NULL REFERENCES users (id),
    assigned_by_id BIGINT NOT NULL REFERENCES users (id),
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    unassigned_at  TIMESTAMPTZ NULL
);

CREATE INDEX idx_incident_assignments_incident_id ON incident_assignments (incident_id);
CREATE INDEX idx_incident_assignments_assigned_to_id ON incident_assignments (assigned_to_id);
