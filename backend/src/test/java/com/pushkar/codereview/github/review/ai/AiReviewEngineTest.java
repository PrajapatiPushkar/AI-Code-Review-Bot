package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiReviewEngineTest {

    private AiReviewEngine aiReviewEngine;

    @BeforeEach
    void setUp() {
        aiReviewEngine = new MockAiReviewEngine();
    }

    @Test
    void testReview_ValidInputReturnsReviewResult() {
        ReviewInput input = new ReviewInput(
                1L, "repo", "owner/repo", "https://github.com/owner/repo", "main",
                10L, 1L, "PR Title", "PR Body", "open", "https://github.com/owner/repo/pull/1",
                "author", "feature", "main", null, null, Collections.emptyList()
        );

        ReviewResult result = aiReviewEngine.review(input);

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo(MockAiReviewEngine.PLACEHOLDER_SUMMARY);
        assertThat(result.getFindings()).isNotNull().isEmpty();
    }

    @Test
    void testReview_NullInputThrowsException() {
        assertThatThrownBy(() -> aiReviewEngine.review(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReviewInput must not be null");
    }
}
