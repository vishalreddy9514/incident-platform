-- V1: Shared trigger function to auto-maintain updated_at columns.
--
-- Rationale: several tables (teams, users, incident_categories, incidents)
-- need an updated_at timestamp that's always correct without every service
-- method remembering to set it manually. A single trigger function, reused
-- via CREATE TRIGGER on each table, is simpler and less error-prone than
-- relying on application code discipline for this.

CREATE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
