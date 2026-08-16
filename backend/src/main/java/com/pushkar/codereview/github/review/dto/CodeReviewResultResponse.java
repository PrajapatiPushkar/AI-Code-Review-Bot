package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;

import java.time.Instant;
import java.util.Objects;

public class CodeReviewResultResponse {

    private Long codeReviewId;
    private Long installationId;
    private String owner;
    private String repository;
    private Integer pullRequestNumber;
    private CodeReviewStatus status;
    private String reviewSummary;
    private Integer totalFindings;
    private Integer postedCommentsCount;
    private Instant createdAt;
    private Instant completedAt;

    public CodeReviewResultResponse() {
    }

    public CodeReviewResultResponse(Long codeReviewId, Long installationId, String owner, String repository,
                                    Integer pullRequestNumber, CodeReviewStatus status, String reviewSummary,
                                    Integer totalFindings, Integer postedCommentsCount,
                                    Instant createdAt, Instant completedAt) {
        this.codeReviewId = codeReviewId;
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.status = status;
        this.reviewSummary = reviewSummary;
        this.totalFindings = totalFindings;
        this.postedCommentsCount = postedCommentsCount;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getCodeReviewId() {
        return codeReviewId;
    }

    public void setCodeReviewId(Long codeReviewId) {
        this.codeReviewId = codeReviewId;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Integer pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public CodeReviewStatus getStatus() {
        return status;
    }

    public void setStatus(CodeReviewStatus status) {
        this.status = status;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public Integer getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(Integer totalFindings) {
        this.totalFindings = totalFindings;
    }

    public Integer getPostedCommentsCount() {
        return postedCommentsCount;
    }

    public void setPostedCommentsCount(Integer postedCommentsCount) {
        this.postedCommentsCount = postedCommentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewResultResponse that = (CodeReviewResultResponse) o;
        return Objects.equals(codeReviewId, that.codeReviewId) &&
                Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                status == that.status &&
                Objects.equals(reviewSummary, that.reviewSummary) &&
                Objects.equals(totalFindings, that.totalFindings) &&
                Objects.equals(postedCommentsCount, that.postedCommentsCount) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeReviewId, installationId, owner, repository, pullRequestNumber,
                status, reviewSummary, totalFindings, postedCommentsCount, createdAt, completedAt);
    }
}
