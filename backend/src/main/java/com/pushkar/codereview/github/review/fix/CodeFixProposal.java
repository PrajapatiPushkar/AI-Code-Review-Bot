package com.pushkar.codereview.github.review.fix;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "code_fix_proposals")
public class CodeFixProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finding_id", nullable = false)
    private Long findingId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "unified_diff", nullable = false, columnDefinition = "TEXT")
    private String unifiedDiff;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "proposed_content", columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "developer_instructions", length = 500)
    private String developerInstructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CodeFixProposalStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CodeFixProposal() {
    }

    public CodeFixProposal(Long findingId, String filePath, String explanation, String unifiedDiff,
                           String originalContent, String proposedContent, String provider,
                           String model, String developerInstructions, CodeFixProposalStatus status) {
        this.findingId = findingId;
        this.filePath = filePath;
        this.explanation = explanation;
        this.unifiedDiff = unifiedDiff;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
        this.provider = provider;
        this.model = model;
        this.developerInstructions = developerInstructions;
        this.status = status != null ? status : CodeFixProposalStatus.PROPOSED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = CodeFixProposalStatus.PROPOSED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFindingId() {
        return findingId;
    }

    public void setFindingId(Long findingId) {
        this.findingId = findingId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getUnifiedDiff() {
        return unifiedDiff;
    }

    public void setUnifiedDiff(String unifiedDiff) {
        this.unifiedDiff = unifiedDiff;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getProposedContent() {
        return proposedContent;
    }

    public void setProposedContent(String proposedContent) {
        this.proposedContent = proposedContent;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDeveloperInstructions() {
        return developerInstructions;
    }

    public void setDeveloperInstructions(String developerInstructions) {
        this.developerInstructions = developerInstructions;
    }

    public CodeFixProposalStatus getStatus() {
        return status;
    }

    public void setStatus(CodeFixProposalStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeFixProposal that = (CodeFixProposal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CodeFixProposal{" +
                "id=" + id +
                ", findingId=" + findingId +
                ", filePath='" + filePath + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
