ALTER TABLE project_alert_config
    ADD COLUMN IF NOT EXISTS discord_webhook_url TEXT;
