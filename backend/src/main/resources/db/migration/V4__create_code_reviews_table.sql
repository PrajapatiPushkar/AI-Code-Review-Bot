-- Flyway Database Migration: V4__create_code_reviews_table.sql
-- Description: Schema creation for code_reviews table

CREATE TABLE code_reviews (
    id BIGSERIAL PRIMARY KEY,
    installation_id BIGINT NOT NULL,
    owner VARCHAR(255) NOT NULL,
    repository VARCHAR(255) NOT NULL,
    pull_request_number INT NOT NULL,
    review_summary TEXT,
    total_findings INT DEFAULT 0,
    posted_comments_count INT DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_code_reviews_owner_repository ON code_reviews(owner, repository);
CREATE INDEX idx_code_reviews_repo_pr ON code_reviews(owner, repository, pull_request_number);
CREATE INDEX idx_code_reviews_created_at ON code_reviews(created_at DESC);
