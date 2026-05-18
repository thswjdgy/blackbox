-- V18: 프로젝트별 경보 임계값 설정 테이블
CREATE TABLE IF NOT EXISTS project_alert_config (
    project_id        BIGINT  NOT NULL PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    imbalance_pct     FLOAT   NOT NULL DEFAULT 40.0,  -- max-min 편차 임계값 (%)
    overload_ratio    FLOAT   NOT NULL DEFAULT 0.60,  -- 1인 과부하 임계값 (비율)
    inactivity_days   INT     NOT NULL DEFAULT 14,    -- 이탈 판단 기간 (일)
    cramming_days     INT     NOT NULL DEFAULT 7,     -- 벼락치기 감지 기간 (일)
    cramming_ratio    FLOAT   NOT NULL DEFAULT 0.60,  -- 벼락치기 임계 비율
    min_events        INT     NOT NULL DEFAULT 10     -- 벼락치기 최소 총 이벤트 수
);
