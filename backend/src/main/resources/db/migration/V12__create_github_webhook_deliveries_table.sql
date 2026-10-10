-- Flyway Database Migration: V12__create_github_webhook_deliveries_table.sql
-- Description: Schema creation for GitHub webhook deliveries idempotency and audit tracking

CREATE TABLE github_webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    delivery_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    action VARCHAR(64),
    repository VARCHAR(255),
    pull_request_number INT,
    commit_sha VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    code_review_id BIGINT,
    error_message VARCHAR(1000),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_webhook_deliveries_delivery_id UNIQUE (delivery_id),
    CONSTRAINT fk_webhook_deliveries_code_review FOREIGN KEY (code_review_id) REFERENCES code_reviews(id) ON DELETE SET NULL
);

CREATE INDEX idx_webhook_deliveries_delivery_id ON github_webhook_deliveries(delivery_id);
CREATE INDEX idx_webhook_deliveries_repo_pr ON github_webhook_deliveries(repository, pull_request_number);
