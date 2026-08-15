package com.pushkar.codereview.github.review.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReviewResult {

    private String summary;
    private List<ReviewFinding> findings;

    public ReviewResult() {
        this.findings = new ArrayList<>();
    }

    public ReviewResult(String summary, List<ReviewFinding> findings) {
        this.summary = summary;
        this.findings = findings != null ? findings : Collections.emptyList();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ReviewFinding> getFindings() {
        return findings != null ? findings : Collections.emptyList();
    }

    public void setFindings(List<ReviewFinding> findings) {
        this.findings = findings != null ? findings : Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewResult that = (ReviewResult) o;
        return Objects.equals(summary, that.summary) &&
                Objects.equals(findings, that.findings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, findings);
    }

    @Override
    public String toString() {
        return "ReviewResult{" +
                "summary='" + summary + '\'' +
                ", findings=" + findings +
                '}';
    }
}
