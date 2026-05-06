-- 프로젝트별 Score Engine 가중치 설정
-- 교수가 프로젝트 특성에 맞게 항목별 가중치를 조정할 수 있다.
-- 기본값: 태스크 35%, 회의 30%, 파일 20%, 외부활동 15%

CREATE TABLE IF NOT EXISTS project_weights (
    project_id  BIGINT       PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    w_task      NUMERIC(5,4) NOT NULL DEFAULT 0.3500,
    w_meeting   NUMERIC(5,4) NOT NULL DEFAULT 0.3000,
    w_file      NUMERIC(5,4) NOT NULL DEFAULT 0.2000,
    w_extra     NUMERIC(5,4) NOT NULL DEFAULT 0.1500,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT project_weights_sum_check CHECK (
        ABS((w_task + w_meeting + w_file + w_extra) - 1.0) < 0.0001
    )
);
