-- V15: file_vault 소프트 삭제 지원
-- 기존 트리거 제거 후 is_deleted 필드만 허용하는 트리거로 교체

ALTER TABLE file_vault
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 기존 트리거 제거
DROP TRIGGER IF EXISTS trg_file_vault_no_update ON file_vault;
DROP TRIGGER IF EXISTS trg_file_vault_no_delete ON file_vault;
DROP FUNCTION IF EXISTS prevent_file_vault_modification();

-- 새 트리거: is_deleted 변경만 허용, 나머지 핵심 컬럼 변조 차단
CREATE OR REPLACE FUNCTION prevent_file_vault_core_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.sha256_hash  <> NEW.sha256_hash  THEN RAISE EXCEPTION 'sha256_hash is immutable'; END IF;
    IF OLD.file_path    <> NEW.file_path    THEN RAISE EXCEPTION 'file_path is immutable'; END IF;
    IF OLD.file_name    <> NEW.file_name    THEN RAISE EXCEPTION 'file_name is immutable'; END IF;
    IF OLD.uploaded_by  <> NEW.uploaded_by  THEN RAISE EXCEPTION 'uploaded_by is immutable'; END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_file_vault_protect_core
    BEFORE UPDATE ON file_vault
    FOR EACH ROW EXECUTE FUNCTION prevent_file_vault_core_modification();

CREATE TRIGGER trg_file_vault_no_delete
    BEFORE DELETE ON file_vault
    FOR EACH ROW EXECUTE FUNCTION prevent_file_vault_core_modification();
