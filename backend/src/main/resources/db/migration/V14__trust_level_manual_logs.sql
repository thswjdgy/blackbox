-- V14: 신뢰도 가중치 + 수동 작업 신고

-- activity_logs: trust_level (자동수집 1.0, 수동신고 0.7)
ALTER TABLE activity_logs
    ADD COLUMN IF NOT EXISTS trust_level FLOAT NOT NULL DEFAULT 1.0;

-- 수동 작업 신고 테이블
CREATE TABLE IF NOT EXISTS manual_logs (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    work_date    DATE        NOT NULL,
    evidence_url VARCHAR(500),
    trust_level  FLOAT       NOT NULL DEFAULT 0.7,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED
    reviewed_by  BIGINT      REFERENCES users(id),
    reviewed_at  TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_manual_logs_project ON manual_logs(project_id);
CREATE INDEX idx_manual_logs_user    ON manual_logs(user_id);
CREATE INDEX idx_manual_logs_status  ON manual_logs(status);

-- 가중치 변경 이력 테이블
CREATE TABLE IF NOT EXISTS weight_history (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    changed_by   BIGINT      NOT NULL REFERENCES users(id),
    w_task       FLOAT       NOT NULL,
    w_meeting    FLOAT       NOT NULL,
    w_file       FLOAT       NOT NULL,
    w_extra      FLOAT       NOT NULL,
    changed_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_weight_history_project ON weight_history(project_id);

-- 피어리뷰 테이블
CREATE TABLE IF NOT EXISTS peer_reviews (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    reviewer_id  BIGINT      NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    reviewee_id  BIGINT      NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    score        INT         NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment      TEXT,
    is_anonymous BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, reviewer_id, reviewee_id)
);

CREATE INDEX idx_peer_reviews_project  ON peer_reviews(project_id);
CREATE INDEX idx_peer_reviews_reviewee ON peer_reviews(reviewee_id);
