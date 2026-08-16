package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;

import java.time.Instant;
import java.util.Objects;

public class CodeReviewHistoryResponse {

    private Long id;
    private Long installationId;
    private String owner;
    private String repository;
    private Integer pullRequestNumber;
    private String reviewSummary;
    private Integer totalFindings;
    private Integer postedCommentsCount;
    private CodeReviewStatus status;
    private Instant createdAt;
    private Instant completedAt;

    public CodeReviewHistoryResponse() {
    }

    public CodeReviewHistoryResponse(Long id, Long installationId, String owner, String repository,
                                    Integer pullRequestNumber, String reviewSummary, Integer totalFindings,
                                    Integer postedCommentsCount, CodeReviewStatus status,
                                    Instant createdAt, Instant completedAt) {
        this.id = id;
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.reviewSummary = reviewSummary;
        this.totalFindings = totalFindings;
        this.postedCommentsCount = postedCommentsCount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CodeReviewStatus getStatus() {
        return status;
    }

    public void setStatus(CodeReviewStatus status) {
        this.status = status;
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
        CodeReviewHistoryResponse that = (CodeReviewHistoryResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(reviewSummary, that.reviewSummary) &&
                Objects.equals(totalFindings, that.totalFindings) &&
                Objects.equals(postedCommentsCount, that.postedCommentsCount) &&
                status == that.status &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, installationId, owner, repository, pullRequestNumber,
                reviewSummary, totalFindings, postedCommentsCount, status, createdAt, completedAt);
    }

    @Override
    public String toString() {
        return "CodeReviewHistoryResponse{" +
                "id=" + id +
                ", installationId=" + installationId +
                ", owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", reviewSummary='" + reviewSummary + '\'' +
                ", totalFindings=" + totalFindings +
                ", postedCommentsCount=" + postedCommentsCount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
