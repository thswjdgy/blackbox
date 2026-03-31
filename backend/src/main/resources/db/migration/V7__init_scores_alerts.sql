CREATE TABLE contribution_scores (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    task_score       FLOAT  NOT NULL DEFAULT 0,
    meeting_score    FLOAT  NOT NULL DEFAULT 0,
    file_score       FLOAT  NOT NULL DEFAULT 0,
    total_score      FLOAT  NOT NULL DEFAULT 0,
    normalized_score FLOAT  NOT NULL DEFAULT 0,
    calculated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);

CREATE TABLE alerts (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    alert_type  VARCHAR(50) NOT NULL,
    severity    VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    message     TEXT        NOT NULL,
    is_resolved BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX idx_scores_project  ON contribution_scores(project_id);
CREATE INDEX idx_alerts_project  ON alerts(project_id);
CREATE INDEX idx_alerts_resolved ON alerts(is_resolved);
