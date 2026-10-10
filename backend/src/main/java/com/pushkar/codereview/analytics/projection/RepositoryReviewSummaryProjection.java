package com.pushkar.codereview.analytics.projection;

import java.time.Instant;

public interface RepositoryReviewSummaryProjection {
    String getOwner();
    String getRepository();
    Long getTotalReviews();
    Long getCompletedReviews();
    Long getFailedReviews();
    Long getInProgressReviews();
    Long getTotalFindings();
    Instant getLastReviewAt();
}
