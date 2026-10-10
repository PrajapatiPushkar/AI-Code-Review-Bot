package com.pushkar.codereview.analytics.dto;

import java.util.Objects;

public class AnalyticsOverviewResponse {

    private long totalReviews;
    private long completedReviews;
    private long failedReviews;
    private long inProgressReviews;
    private long totalFindings;

    public AnalyticsOverviewResponse() {
    }

    public AnalyticsOverviewResponse(long totalReviews, long completedReviews, long failedReviews,
                                     long inProgressReviews, long totalFindings) {
        this.totalReviews = totalReviews;
        this.completedReviews = completedReviews;
        this.failedReviews = failedReviews;
        this.inProgressReviews = inProgressReviews;
        this.totalFindings = totalFindings;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public long getCompletedReviews() {
        return completedReviews;
    }

    public void setCompletedReviews(long completedReviews) {
        this.completedReviews = completedReviews;
    }

    public long getFailedReviews() {
        return failedReviews;
    }

    public void setFailedReviews(long failedReviews) {
        this.failedReviews = failedReviews;
    }

    public long getInProgressReviews() {
        return inProgressReviews;
    }

    public void setInProgressReviews(long inProgressReviews) {
        this.inProgressReviews = inProgressReviews;
    }

    public long getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(long totalFindings) {
        this.totalFindings = totalFindings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsOverviewResponse that = (AnalyticsOverviewResponse) o;
        return totalReviews == that.totalReviews &&
                completedReviews == that.completedReviews &&
                failedReviews == that.failedReviews &&
                inProgressReviews == that.inProgressReviews &&
                totalFindings == that.totalFindings;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalReviews, completedReviews, failedReviews, inProgressReviews, totalFindings);
    }
}
