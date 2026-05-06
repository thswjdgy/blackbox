-- V16: manual_logs 에 review_note 컬럼 추가
ALTER TABLE manual_logs
    ADD COLUMN IF NOT EXISTS review_note TEXT;
