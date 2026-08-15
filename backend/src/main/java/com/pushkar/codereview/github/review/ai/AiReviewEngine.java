package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;

public interface AiReviewEngine {

    ReviewResult review(ReviewInput input);
}
