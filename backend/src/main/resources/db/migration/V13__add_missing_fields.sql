-- V13: ERD 기준 누락 필드 추가
-- meetings: ai_summary, notion_page_id
ALTER TABLE meetings
    ADD COLUMN IF NOT EXISTS ai_summary    TEXT,
    ADD COLUMN IF NOT EXISTS notion_page_id VARCHAR(255);

-- projects: course_name, semester, start_date, end_date, created_by
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS course_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS semester    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS start_date  DATE,
    ADD COLUMN IF NOT EXISTS end_date    DATE,
    ADD COLUMN IF NOT EXISTS created_by  BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- project_members: 외부 서비스 동의 세분화
ALTER TABLE project_members
    ADD COLUMN IF NOT EXISTS consent_github BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consent_drive  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consent_ai     BOOLEAN NOT NULL DEFAULT FALSE;
