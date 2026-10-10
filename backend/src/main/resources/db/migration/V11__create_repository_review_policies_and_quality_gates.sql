-- Flyway Database Migration: V11__create_repository_review_policies_and_quality_gates.sql
-- Description: Schema creation for repository review policies and code review quality gates

CREATE TABLE repository_review_policies (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fail_on_severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    enabled_rule_ids VARCHAR(1000) NOT NULL DEFAULT 'RULE-JAVA-SYSTEM-OUT,RULE-JAVA-EMPTY-CATCH,RULE-TODO-FIXME',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_repository_review_policies_repo_id UNIQUE (repository_id),
    CONSTRAINT fk_repository_review_policies_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
);

CREATE INDEX idx_repository_review_policies_repo_id ON repository_review_policies(repository_id);

CREATE TABLE code_review_quality_gates (
    id BIGSERIAL PRIMARY KEY,
    code_review_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    gate_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fail_on_severity VARCHAR(20) NOT NULL,
    failure_count INT NOT NULL DEFAULT 0,
    reason VARCHAR(1000),
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_code_review_quality_gates_review_id UNIQUE (code_review_id),
    CONSTRAINT fk_code_review_quality_gates_review FOREIGN KEY (code_review_id) REFERENCES code_reviews(id) ON DELETE CASCADE
);

CREATE INDEX idx_code_review_quality_gates_review_id ON code_review_quality_gates(code_review_id);
