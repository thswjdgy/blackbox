ALTER TABLE project_alert_config
    ADD COLUMN IF NOT EXISTS deadline_days INT NOT NULL DEFAULT 3;
