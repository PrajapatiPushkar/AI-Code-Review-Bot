package com.pushkar.codereview.analytics.dto;

import java.util.Map;
import java.util.Objects;

public class AnalyticsFindingsResponse {

    private long totalFindings;
    private Map<String, Long> severityBreakdown;
    private Map<String, Long> categoryBreakdown;
    private Map<String, Long> sourceBreakdown;

    public AnalyticsFindingsResponse() {
    }

    public AnalyticsFindingsResponse(long totalFindings,
                                     Map<String, Long> severityBreakdown,
                                     Map<String, Long> categoryBreakdown,
                                     Map<String, Long> sourceBreakdown) {
        this.totalFindings = totalFindings;
        this.severityBreakdown = severityBreakdown;
        this.categoryBreakdown = categoryBreakdown;
        this.sourceBreakdown = sourceBreakdown;
    }

    public long getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(long totalFindings) {
        this.totalFindings = totalFindings;
    }

    public Map<String, Long> getSeverityBreakdown() {
        return severityBreakdown;
    }

    public void setSeverityBreakdown(Map<String, Long> severityBreakdown) {
        this.severityBreakdown = severityBreakdown;
    }

    public Map<String, Long> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, Long> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public Map<String, Long> getSourceBreakdown() {
        return sourceBreakdown;
    }

    public void setSourceBreakdown(Map<String, Long> sourceBreakdown) {
        this.sourceBreakdown = sourceBreakdown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsFindingsResponse that = (AnalyticsFindingsResponse) o;
        return totalFindings == that.totalFindings &&
                Objects.equals(severityBreakdown, that.severityBreakdown) &&
                Objects.equals(categoryBreakdown, that.categoryBreakdown) &&
                Objects.equals(sourceBreakdown, that.sourceBreakdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalFindings, severityBreakdown, categoryBreakdown, sourceBreakdown);
    }
}
