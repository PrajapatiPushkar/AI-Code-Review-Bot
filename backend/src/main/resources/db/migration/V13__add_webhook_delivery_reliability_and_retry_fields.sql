-- Flyway Database Migration: V13__add_webhook_delivery_reliability_and_retry_fields.sql
-- Description: Add reliability, retry management, and user ownership fields to github_webhook_deliveries

ALTER TABLE github_webhook_deliveries
    ADD COLUMN user_id BIGINT,
    ADD COLUMN installation_id BIGINT,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 1,
    ADD COLUMN max_attempts INT NOT NULL DEFAULT 3,
    ADD COLUMN error_category VARCHAR(64),
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE github_webhook_deliveries
    ADD CONSTRAINT fk_webhook_deliveries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_webhook_deliveries_user_id ON github_webhook_deliveries(user_id);
CREATE INDEX idx_webhook_deliveries_status ON github_webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_retry ON github_webhook_deliveries(status, retryable, next_retry_at);
