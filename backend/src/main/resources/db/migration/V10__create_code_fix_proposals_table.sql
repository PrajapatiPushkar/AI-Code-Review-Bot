-- V10: Create code_fix_proposals table for persistent AI-generated fix history and patch artifacts
CREATE TABLE code_fix_proposals (
    id BIGSERIAL PRIMARY KEY,
    finding_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    explanation TEXT,
    unified_diff TEXT NOT NULL,
    original_content TEXT,
    proposed_content TEXT,
    provider VARCHAR(50),
    model VARCHAR(100),
    developer_instructions VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_code_fix_proposals_finding FOREIGN KEY (finding_id) REFERENCES code_review_findings(id) ON DELETE CASCADE
);

CREATE INDEX idx_code_fix_proposals_finding_id ON code_fix_proposals(finding_id);
