-- Flyway Database Migration: V7__add_github_installation_verification.sql
-- Description: Add verification status and verification timestamp to github_installations table

ALTER TABLE github_installations
ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN verified_at TIMESTAMP WITH TIME ZONE;
