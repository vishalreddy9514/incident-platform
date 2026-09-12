-- V3: users
--
-- role is a plain VARCHAR with a CHECK constraint rather than a native
-- Postgres ENUM type. A native enum requires ALTER TYPE ... ADD VALUE for
-- new values, which historically couldn't run inside a transaction with
-- other DDL in the same migration in older Postgres versions, and is a
-- less common pattern to explain/defend than a CHECK constraint. See
-- ADR-0006 for the full reasoning; this applies to every constrained
-- string column in this schema (role, status, priority, severity).

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(150) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    team_id       BIGINT NULL REFERENCES teams (id) ON DELETE SET NULL,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ENGINEER', 'ADMIN'))
);

CREATE INDEX idx_users_team_id ON users (team_id);
CREATE INDEX idx_users_role ON users (role);

CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
