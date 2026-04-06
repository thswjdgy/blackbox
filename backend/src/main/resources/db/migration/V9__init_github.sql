-- GitHub 레포 연동 정보 (프로젝트당 1개)
CREATE TABLE github_installations (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repo_full_name  VARCHAR(255) NOT NULL,   -- e.g. "owner/repo"
    github_token    VARCHAR(512),            -- Personal Access Token (폴링용)
    webhook_secret  VARCHAR(255),            -- Webhook HMAC 검증 시크릿
    last_polled_at  TIMESTAMP,
    connected_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id)
);

-- GitHub 로그인 ↔ 플랫폼 유저 수동 매핑
CREATE TABLE github_user_mappings (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_login  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id),
    UNIQUE(project_id, github_login)
);

CREATE INDEX idx_github_installations_project ON github_installations(project_id);
CREATE INDEX idx_github_user_mappings_project  ON github_user_mappings(project_id);
