-- V8: incident_history
--
-- General-purpose timeline feed for the incident detail UI (FR-11), as
-- distinct from incident_assignments (Phase 1 §8.2). Declared append-only
-- in the design — enforced here with a trigger, not just left as a
-- documentation comment, since a table that's supposed to be an audit
-- trail is only actually trustworthy if the database itself prevents
-- tampering, not just the application layer.

CREATE TABLE incident_history (
    id            BIGSERIAL PRIMARY KEY,
    incident_id   BIGINT NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    actor_id      BIGINT NOT NULL REFERENCES users (id),
    field_changed VARCHAR(50) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_history_incident_id ON incident_history (incident_id);

-- Generic append-only guard, reused by any table that must never be
-- updated or deleted post-insert (also applied to audit_logs in V9).
CREATE FUNCTION prevent_append_only_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% is append-only: % is not permitted', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_incident_history_no_update
    BEFORE UPDATE ON incident_history
    FOR EACH ROW
    EXECUTE FUNCTION prevent_append_only_mutation();

CREATE TRIGGER trg_incident_history_no_delete
    BEFORE DELETE ON incident_history
    FOR EACH ROW
    EXECUTE FUNCTION prevent_append_only_mutation();
