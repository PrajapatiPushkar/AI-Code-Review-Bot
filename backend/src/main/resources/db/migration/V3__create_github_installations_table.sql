-- Flyway Database Migration: V3__create_github_installations_table.sql
-- Description: Schema creation for github_installations table with foreign key constraint to users table

CREATE TABLE github_installations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    github_installation_id BIGINT NOT NULL,
    github_account_login VARCHAR(255) NOT NULL,
    github_account_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_github_installations_installation_id UNIQUE (github_installation_id),
    CONSTRAINT fk_github_installations_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for fast lookup
CREATE INDEX idx_github_installations_user_id ON github_installations(user_id);
CREATE INDEX idx_github_installations_installation_id ON github_installations(github_installation_id);
