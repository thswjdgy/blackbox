CREATE TABLE IF NOT EXISTS ai_analysis_results (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    analysis_type VARCHAR(20) NOT NULL,  -- 'TEAM' | 'COMMIT'
    result        TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_analysis_project_type ON ai_analysis_results(project_id, analysis_type, created_at DESC);
