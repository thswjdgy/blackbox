CREATE TABLE file_vault (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    uploaded_by BIGINT       NOT NULL REFERENCES users(id),
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_size   BIGINT       NOT NULL,
    mime_type   VARCHAR(100),
    sha256_hash VARCHAR(64)  NOT NULL,
    version     INT          NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tamper_detection_log (
    id            BIGSERIAL PRIMARY KEY,
    vault_id      BIGINT      NOT NULL REFERENCES file_vault(id),
    expected_hash VARCHAR(64) NOT NULL,
    actual_hash   VARCHAR(64) NOT NULL,
    detected_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 불변성 트리거 (UPDATE/DELETE 차단)
CREATE OR REPLACE FUNCTION prevent_file_vault_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'file_vault is immutable. INSERT only.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_file_vault_no_update
    BEFORE UPDATE ON file_vault
    FOR EACH ROW EXECUTE FUNCTION prevent_file_vault_modification();

CREATE TRIGGER trg_file_vault_no_delete
    BEFORE DELETE ON file_vault
    FOR EACH ROW EXECUTE FUNCTION prevent_file_vault_modification();

CREATE INDEX idx_file_vault_project ON file_vault(project_id);
CREATE INDEX idx_file_vault_hash    ON file_vault(sha256_hash);
