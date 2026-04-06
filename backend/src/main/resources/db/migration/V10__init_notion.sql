-- Notion 연동 테이블
CREATE TABLE notion_installations (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    integration_token VARCHAR(512)  NOT NULL,
    database_id      VARCHAR(255),            -- 특정 DB만 폴링할 때 사용 (없으면 전체 검색)
    workspace_name   VARCHAR(255),
    last_polled_at   TIMESTAMP,
    connected_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE(project_id)
);

-- Notion 유저 매핑 (Notion User ID → 플랫폼 User)
CREATE TABLE notion_user_mappings (
    id              BIGSERIAL    PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    notion_user_id  VARCHAR(100) NOT NULL,
    notion_user_name VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id),
    UNIQUE(project_id, notion_user_id)
);

CREATE INDEX idx_notion_installations_project ON notion_installations(project_id);
CREATE INDEX idx_notion_user_mappings_project  ON notion_user_mappings(project_id);
