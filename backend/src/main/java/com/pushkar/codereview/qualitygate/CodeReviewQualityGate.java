package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "code_review_quality_gates")
public class CodeReviewQualityGate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code_review_id", nullable = false, unique = true)
    private CodeReview codeReview;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QualityGateStatus status;

    @Column(name = "gate_enabled", nullable = false)
    private Boolean gateEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_on_severity", nullable = false, length = 20)
    private ReviewFindingSeverity failOnSeverity;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    public CodeReviewQualityGate() {
    }

    public CodeReviewQualityGate(CodeReview codeReview, QualityGateStatus status, Boolean gateEnabled,
                                 ReviewFindingSeverity failOnSeverity, Integer failureCount,
                                 String reason, Instant evaluatedAt) {
        this.codeReview = codeReview;
        this.status = status;
        this.gateEnabled = gateEnabled != null ? gateEnabled : true;
        this.failOnSeverity = failOnSeverity;
        this.failureCount = failureCount != null ? failureCount : 0;
        this.reason = reason;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.evaluatedAt == null) {
            this.evaluatedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CodeReview getCodeReview() {
        return codeReview;
    }

    public void setCodeReview(CodeReview codeReview) {
        this.codeReview = codeReview;
    }

    public QualityGateStatus getStatus() {
        return status;
    }

    public void setStatus(QualityGateStatus status) {
        this.status = status;
    }

    public Boolean getGateEnabled() {
        return gateEnabled;
    }

    public boolean isGateEnabled() {
        return Boolean.TRUE.equals(this.gateEnabled);
    }

    public void setGateEnabled(Boolean gateEnabled) {
        this.gateEnabled = gateEnabled;
    }

    public ReviewFindingSeverity getFailOnSeverity() {
        return failOnSeverity;
    }

    public void setFailOnSeverity(ReviewFindingSeverity failOnSeverity) {
        this.failOnSeverity = failOnSeverity;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewQualityGate that = (CodeReviewQualityGate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
