package com.pushkar.codereview.github.review.persistence;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "code_review_findings")
public class CodeReviewFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_review_id", nullable = false)
    private CodeReview codeReview;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "end_line_number")
    private Integer endLineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ReviewFindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private ReviewFindingCategory category;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CodeReviewFinding() {
    }

    public CodeReviewFinding(CodeReview codeReview, String filePath, Integer lineNumber, Integer endLineNumber,
                              ReviewFindingSeverity severity, ReviewFindingCategory category,
                              String message, String suggestion) {
        this.codeReview = codeReview;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.suggestion = suggestion;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getEndLineNumber() {
        return endLineNumber;
    }

    public void setEndLineNumber(Integer endLineNumber) {
        this.endLineNumber = endLineNumber;
    }

    public ReviewFindingSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ReviewFindingSeverity severity) {
        this.severity = severity;
    }

    public ReviewFindingCategory getCategory() {
        return category;
    }

    public void setCategory(ReviewFindingCategory category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewFinding that = (CodeReviewFinding) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
