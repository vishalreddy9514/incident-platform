-- V4: incident_categories
-- Admin-managed reference data (FR-21). Deactivation is soft (is_active
-- flag) rather than deletion, so historical incidents referencing a
-- retired category keep a valid, meaningful foreign key.

CREATE TABLE incident_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_incident_categories_name UNIQUE (name)
);

CREATE TRIGGER trg_incident_categories_set_updated_at
    BEFORE UPDATE ON incident_categories
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
