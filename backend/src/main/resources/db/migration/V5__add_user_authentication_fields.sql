-- Flyway Database Migration: V5__add_user_authentication_fields.sql
-- Description: Add password_hash, enabled columns and allow nullable github_id for local authentication

ALTER TABLE users ALTER COLUMN github_id DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
