package com.pushkar.codereview.analytics.projection;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;

public interface FindingCategoryGroupProjection {
    ReviewFindingCategory getCategory();
    Long getCount();
}
