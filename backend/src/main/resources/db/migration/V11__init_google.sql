-- Google OAuth 연동 정보 (프로젝트별)
CREATE TABLE google_installations (
    id                BIGSERIAL PRIMARY KEY,
    project_id        BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    access_token      TEXT,
    refresh_token     TEXT         NOT NULL,
    token_expires_at  TIMESTAMP,
    -- 연동할 리소스 ID (선택적)
    drive_folder_id   VARCHAR(255),   -- 특정 폴더만 폴링할 때
    sheet_id          VARCHAR(255),   -- 특정 시트만 폴링할 때
    form_id           VARCHAR(255),   -- 특정 폼만 폴링할 때
    -- 마지막 폴링 시각
    drive_polled_at   TIMESTAMP,
    sheets_polled_at  TIMESTAMP,
    forms_polled_at   TIMESTAMP,
    connected_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(project_id)
);

-- Google 유저 매핑 (Google 계정 ↔ 플랫폼 유저)
CREATE TABLE google_user_mappings (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    google_email    VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id),
    UNIQUE(project_id, google_email)
);
