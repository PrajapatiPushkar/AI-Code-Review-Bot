-- V9: Add commit_sha column to code_reviews table for revision-aware idempotency
ALTER TABLE code_reviews ADD COLUMN IF NOT EXISTS commit_sha VARCHAR(255);

-- Create index on dedup fields for fast duplicate lookups
CREATE INDEX IF NOT EXISTS idx_code_reviews_dedup ON code_reviews(user_id, installation_id, owner, repository, pull_request_number, commit_sha);
