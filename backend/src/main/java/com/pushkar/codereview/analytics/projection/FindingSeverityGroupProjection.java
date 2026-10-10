package com.pushkar.codereview.analytics.projection;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;

public interface FindingSeverityGroupProjection {
    ReviewFindingSeverity getSeverity();
    Long getCount();
}
