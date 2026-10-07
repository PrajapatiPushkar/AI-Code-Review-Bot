package com.pushkar.codereview.github.review.dto;

import java.time.Instant;
import java.util.Objects;

public class CodeFixResponse {

    private Long findingId;
    private String filePath;
    private String explanation;
    private String unifiedDiff;
    private String originalContent;
    private String proposedContent;
    private Instant generatedAt;
    private String provider;
    private String status;
    private String limitations;

    public CodeFixResponse() {
    }

    public CodeFixResponse(Long findingId, String filePath, String explanation, String unifiedDiff,
                           String originalContent, String proposedContent, Instant generatedAt,
                           String provider, String status, String limitations) {
        this.findingId = findingId;
        this.filePath = filePath;
        this.explanation = explanation;
        this.unifiedDiff = unifiedDiff;
        this.originalContent = originalContent;
        this.proposedContent = proposedContent;
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
        this.provider = provider != null ? provider : "Gemini";
        this.status = status != null ? status : "PROPOSED";
        this.limitations = limitations;
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

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeFixResponse that = (CodeFixResponse) o;
        return Objects.equals(findingId, that.findingId) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(unifiedDiff, that.unifiedDiff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(findingId, filePath, unifiedDiff);
    }
}
