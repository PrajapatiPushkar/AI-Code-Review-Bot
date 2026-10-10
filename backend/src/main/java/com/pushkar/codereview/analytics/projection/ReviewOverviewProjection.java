package com.pushkar.codereview.analytics.projection;

public interface ReviewOverviewProjection {
    Long getTotalReviews();
    Long getCompletedReviews();
    Long getFailedReviews();
    Long getInProgressReviews();
    Long getTotalFindings();
}
