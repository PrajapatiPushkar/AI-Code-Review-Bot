package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.springframework.stereotype.Service;

@Service
public class AiReviewService {

    private final AiReviewEngine aiReviewEngine;

    public AiReviewService(AiReviewEngine aiReviewEngine) {
        this.aiReviewEngine = aiReviewEngine;
    }

    public ReviewResult review(ReviewInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ReviewInput must not be null");
        }
        return aiReviewEngine.review(input);
    }
}
