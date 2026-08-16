package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;

import java.time.Instant;
import java.util.Objects;

public class CodeReviewStatusResponse {

    private Long codeReviewId;
    private CodeReviewStatus status;
    private Instant createdAt;
    private Instant completedAt;
    private Integer totalFindings;
    private Integer postedCommentsCount;
    private String reviewSummary;

    public CodeReviewStatusResponse() {
    }

    public CodeReviewStatusResponse(Long codeReviewId, CodeReviewStatus status, Instant createdAt, Instant completedAt,
                                    Integer totalFindings, Integer postedCommentsCount, String reviewSummary) {
        this.codeReviewId = codeReviewId;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.totalFindings = totalFindings;
        this.postedCommentsCount = postedCommentsCount;
        this.reviewSummary = reviewSummary;
    }

    public Long getCodeReviewId() {
        return codeReviewId;
    }

    public void setCodeReviewId(Long codeReviewId) {
        this.codeReviewId = codeReviewId;
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

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewStatusResponse that = (CodeReviewStatusResponse) o;
        return Objects.equals(codeReviewId, that.codeReviewId) &&
                status == that.status &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(completedAt, that.completedAt) &&
                Objects.equals(totalFindings, that.totalFindings) &&
                Objects.equals(postedCommentsCount, that.postedCommentsCount) &&
                Objects.equals(reviewSummary, that.reviewSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeReviewId, status, createdAt, completedAt, totalFindings, postedCommentsCount, reviewSummary);
    }
}
