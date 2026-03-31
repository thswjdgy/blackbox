CREATE TABLE meetings (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    purpose      TEXT,
    notes        TEXT,
    decisions    TEXT,
    checkin_code VARCHAR(20),
    meeting_at   TIMESTAMP    NOT NULL,
    created_by   BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE meeting_attendees (
    meeting_id    BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    checked_in_at TIMESTAMP,
    PRIMARY KEY (meeting_id, user_id)
);

-- tasks.meeting_id FK (meetings 테이블 생성 이후 추가)
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_meeting
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE SET NULL;

CREATE INDEX idx_meetings_project ON meetings(project_id);
