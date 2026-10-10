package com.pushkar.codereview.analytics.projection;

import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;

import java.time.Instant;

public interface ReviewTrendRowProjection {
    Instant getCreatedAt();
    CodeReviewStatus getStatus();
    Integer getTotalFindings();
}
