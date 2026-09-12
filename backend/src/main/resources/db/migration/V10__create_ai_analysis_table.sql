-- V10: ai_analysis
--
-- Stores the result of each AI analysis request (FR-13). One incident can
-- have multiple AIAnalysis rows over time (re-analysis after more comments
-- are added, for example) — no unique constraint on incident_id, and the
-- most recent row per incident is what the "current" analysis panel shows.
-- keywords/suggested_steps are JSONB since they're variable-length lists
-- with no independent query need of their own (no FR requires searching
-- incidents by AI-suggested keyword yet — a reasonable future improvement,
-- not built speculatively now).

CREATE TABLE ai_analysis (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    suggested_category  VARCHAR(100),
    predicted_priority  VARCHAR(10),
    summary             TEXT,
    keywords            JSONB,
    suggested_steps     JSONB,
    model_used          VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_analysis_predicted_priority CHECK (
        predicted_priority IS NULL OR predicted_priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    )
);

CREATE INDEX idx_ai_analysis_incident_id ON ai_analysis (incident_id);
