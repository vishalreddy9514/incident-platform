-- V2: teams
-- Referenced by users (membership) and incidents (ownership) — created
-- first since both later tables have an optional foreign key to it.

CREATE TABLE teams (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_teams_name UNIQUE (name)
);

CREATE TRIGGER trg_teams_set_updated_at
    BEFORE UPDATE ON teams
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
