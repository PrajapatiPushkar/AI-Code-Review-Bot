package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.review.fix.CodeFixProposal;
import com.pushkar.codereview.github.review.fix.CodeFixProposalStatus;

import java.time.Instant;
import java.util.Objects;

public class CodeFixProposalResponse {

    private Long id;
    private Long findingId;
    private String filePath;
    private String explanation;
    private String unifiedDiff;
    private String originalContent;
    private String proposedContent;
    private String developerInstructions;
    private String provider;
    private String model;
    private CodeFixProposalStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public CodeFixProposalResponse() {
    }

    public CodeFixProposalResponse(Long id, Long findingId, String filePath, String explanation,
                                   String unifiedDiff, String originalContent, String proposedContent,
                                   String developerInstructions, String provider, String model,
                                   CodeFixProposalStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.findingId = findingId;
        this.filePath = filePath;
        this.explanation = explanation;
        this.unifiedDiff = unifiedDiff;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
        this.developerInstructions = developerInstructions;
        this.provider = provider;
        this.model = model;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CodeFixProposalResponse fromEntity(CodeFixProposal proposal) {
        if (proposal == null) {
            return null;
        }
        return new CodeFixProposalResponse(
                proposal.getId(),
                proposal.getFindingId(),
                proposal.getFilePath(),
                proposal.getExplanation(),
                proposal.getUnifiedDiff(),
                proposal.getOriginalContent(),
                proposal.getProposedContent(),
                proposal.getDeveloperInstructions(),
                proposal.getProvider(),
                proposal.getModel(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
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

    public String getDeveloperInstructions() {
        return developerInstructions;
    }

    public void setDeveloperInstructions(String developerInstructions) {
        this.developerInstructions = developerInstructions;
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
        CodeFixProposalResponse that = (CodeFixProposalResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(findingId, that.findingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, findingId);
    }
}
