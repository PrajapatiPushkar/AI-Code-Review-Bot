package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiReviewServiceTest {

    private StubAiReviewEngine stubEngine;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        stubEngine = new StubAiReviewEngine();
        aiReviewService = new AiReviewService(stubEngine);
    }

    @Test
    void testReview_DelegatesToEngineAndReturnsResult() {
        ReviewInput input = new ReviewInput(
                1L, "repo", "owner/repo", "https://github.com/owner/repo", "main",
                10L, 1L, "PR Title", "PR Body", "open", "https://github.com/owner/repo/pull/1",
                "author", "feature", "main", null, null, Collections.emptyList()
        );

        ReviewFinding finding = new ReviewFinding("src/Main.java", 15, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Null pointer issue", "Add null check");
        ReviewResult expectedResult = new ReviewResult("Custom engine review summary", List.of(finding));

        stubEngine.setResponse(expectedResult);

        ReviewResult result = aiReviewService.review(input);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getSummary()).isEqualTo("Custom engine review summary");
        assertThat(result.getFindings()).hasSize(1);
        assertThat(result.getFindings().get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(stubEngine.isCalled()).isTrue();
        assertThat(stubEngine.getReceivedInput()).isEqualTo(input);
    }

    @Test
    void testReview_PropagatesEngineException() {
        ReviewInput input = new ReviewInput();
        stubEngine.setException(new RuntimeException("AI Provider Service Unavailable"));

        assertThatThrownBy(() -> aiReviewService.review(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI Provider Service Unavailable");

        assertThat(stubEngine.isCalled()).isTrue();
    }

    @Test
    void testReview_NullInputHandledConsistently() {
        assertThatThrownBy(() -> aiReviewService.review(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReviewInput must not be null");

        assertThat(stubEngine.isCalled()).isFalse();
    }

    // --- Helper Stub ---

    private static class StubAiReviewEngine implements AiReviewEngine {
        private ReviewResult response;
        private RuntimeException exception;
        private boolean called = false;
        private ReviewInput receivedInput;

        public void setResponse(ReviewResult response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public boolean isCalled() {
            return called;
        }

        public ReviewInput getReceivedInput() {
            return receivedInput;
        }

        @Override
        public ReviewResult review(ReviewInput input) {
            this.called = true;
            this.receivedInput = input;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
