CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    invite_code VARCHAR(20)   NOT NULL UNIQUE,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE project_members (
    id                      BIGSERIAL PRIMARY KEY,
    project_id              BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id                 BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    project_role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    data_collection_consent BOOLEAN     NOT NULL DEFAULT FALSE,
    consent_at              TIMESTAMP,
    joined_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);

CREATE INDEX idx_project_members_project ON project_members(project_id);
CREATE INDEX idx_project_members_user    ON project_members(user_id);
