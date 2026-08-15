package com.pushkar.codereview.github.review.dto;

import java.util.Objects;

public class ReviewFinding {

    private String filename;
    private Integer line;
    private ReviewFindingSeverity severity;
    private ReviewFindingCategory category;
    private String message;
    private String suggestion;

    public ReviewFinding() {
    }

    public ReviewFinding(String filename, Integer line, ReviewFindingSeverity severity,
                         ReviewFindingCategory category, String message, String suggestion) {
        this.filename = filename;
        this.line = line;
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.suggestion = suggestion;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewFinding that = (ReviewFinding) o;
        return Objects.equals(filename, that.filename) &&
                Objects.equals(line, that.line) &&
                severity == that.severity &&
                category == that.category &&
                Objects.equals(message, that.message) &&
                Objects.equals(suggestion, that.suggestion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, line, severity, category, message, suggestion);
    }

    @Override
    public String toString() {
        return "ReviewFinding{" +
                "filename='" + filename + '\'' +
                ", line=" + line +
                ", severity=" + severity +
                ", category=" + category +
                ", message='" + message + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}
