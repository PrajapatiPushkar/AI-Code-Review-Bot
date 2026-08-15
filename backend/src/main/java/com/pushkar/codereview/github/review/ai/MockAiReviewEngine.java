package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class MockAiReviewEngine implements AiReviewEngine {

    public static final String PLACEHOLDER_SUMMARY = "AI review engine placeholder executed successfully.";

    @Override
    public ReviewResult review(ReviewInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ReviewInput must not be null");
        }

        return new ReviewResult(PLACEHOLDER_SUMMARY, Collections.emptyList());
    }
}
