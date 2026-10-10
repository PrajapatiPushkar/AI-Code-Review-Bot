package com.pushkar.codereview.analytics.projection;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;

public interface RepositoryFindingSeverityProjection {
    String getOwner();
    String getRepository();
    ReviewFindingSeverity getSeverity();
    Long getCount();
}
