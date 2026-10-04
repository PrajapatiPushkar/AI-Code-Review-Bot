package com.pushkar.codereview.github.review.dto;

import java.time.Instant;
import java.util.Objects;

public class CodeReviewFindingResponse {

    private Long id;
    private String filePath;
    private Integer lineNumber;
    private Integer endLineNumber;
    private String severity;
    private String category;
    private String message;
    private String suggestion;
    private Instant createdAt;
    private String source = "AI";
    private String ruleId;

    public CodeReviewFindingResponse() {
    }

    public CodeReviewFindingResponse(Long id, String filePath, Integer lineNumber, Integer endLineNumber,
                                     String severity, String category, String message, String suggestion,
                                     Instant createdAt) {
        this(id, filePath, lineNumber, endLineNumber, severity, category, message, suggestion, createdAt, "AI", null);
    }

    public CodeReviewFindingResponse(Long id, String filePath, Integer lineNumber, Integer endLineNumber,
                                     String severity, String category, String message, String suggestion,
                                     Instant createdAt, String source, String ruleId) {
        this.id = id;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.suggestion = suggestion;
        this.createdAt = createdAt;
        this.source = source != null ? source : "AI";
        this.ruleId = ruleId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source != null ? source : "AI";
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewFindingResponse that = (CodeReviewFindingResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(lineNumber, that.lineNumber) &&
                Objects.equals(endLineNumber, that.endLineNumber) &&
                Objects.equals(severity, that.severity) &&
                Objects.equals(category, that.category) &&
                Objects.equals(message, that.message) &&
                Objects.equals(suggestion, that.suggestion) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(source, that.source) &&
                Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath, lineNumber, endLineNumber, severity, category, message, suggestion, createdAt, source, ruleId);
    }
}
