-- V6: Add user_id column to code_reviews table for user ownership tracking
ALTER TABLE code_reviews ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Add foreign key constraint to users table
ALTER TABLE code_reviews
    ADD CONSTRAINT fk_code_reviews_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL;

-- Create index on user_id for fast user review lookups
CREATE INDEX IF NOT EXISTS idx_code_reviews_user_id ON code_reviews(user_id);
