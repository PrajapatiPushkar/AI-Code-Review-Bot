package com.pushkar.codereview.analytics.dto;

import java.util.Objects;

public class AnalyticsTrendItemResponse {

    private String date;
    private long totalReviews;
    private long completedReviews;
    private long failedReviews;
    private long inProgressReviews;
    private long totalFindings;

    public AnalyticsTrendItemResponse() {
    }

    public AnalyticsTrendItemResponse(String date, long totalReviews, long completedReviews,
                                      long failedReviews, long inProgressReviews, long totalFindings) {
        this.date = date;
        this.totalReviews = totalReviews;
        this.completedReviews = completedReviews;
        this.failedReviews = failedReviews;
        this.inProgressReviews = inProgressReviews;
        this.totalFindings = totalFindings;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
        AnalyticsTrendItemResponse that = (AnalyticsTrendItemResponse) o;
        return totalReviews == that.totalReviews &&
                completedReviews == that.completedReviews &&
                failedReviews == that.failedReviews &&
                inProgressReviews == that.inProgressReviews &&
                totalFindings == that.totalFindings &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, totalReviews, completedReviews, failedReviews, inProgressReviews, totalFindings);
    }
}
