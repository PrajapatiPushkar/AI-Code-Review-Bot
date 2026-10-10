package com.pushkar.codereview.qualitygate.dto;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.qualitygate.QualityGateStatus;

import java.time.Instant;

public class QualityGateResponse {

    private Long id;
    private Long reviewId;
    private QualityGateStatus status;
    private boolean enabled;
    private ReviewFindingSeverity failOnSeverity;
    private int failureCount;
    private String reason;
    private Instant evaluatedAt;

    public QualityGateResponse() {
    }

    public QualityGateResponse(Long id, Long reviewId, QualityGateStatus status, boolean enabled,
                               ReviewFindingSeverity failOnSeverity, int failureCount, String reason, Instant evaluatedAt) {
        this.id = id;
        this.reviewId = reviewId;
        this.status = status;
        this.enabled = enabled;
        this.failOnSeverity = failOnSeverity;
        this.failureCount = failureCount;
        this.reason = reason;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public QualityGateStatus getStatus() {
        return status;
    }

    public void setStatus(QualityGateStatus status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ReviewFindingSeverity getFailOnSeverity() {
        return failOnSeverity;
    }

    public void setFailOnSeverity(ReviewFindingSeverity failOnSeverity) {
        this.failOnSeverity = failOnSeverity;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
