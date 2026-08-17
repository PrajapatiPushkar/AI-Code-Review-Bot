package com.pushkar.codereview.config;

import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.AsyncCodeReviewRunner;
import com.pushkar.codereview.github.review.GithubPullRequestReviewService;
import com.pushkar.codereview.github.review.GithubReviewCommentService;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCorrelationTest {

    private StubPullRequestReviewService pullRequestReviewService;
    private StubAiReviewService aiReviewService;
    private StubReviewCommentService reviewCommentService;
    private StubPersistenceService persistenceService;
    private CodeReviewMetrics metrics;
    private AsyncCodeReviewRunner runner;

    @BeforeEach
    void setUp() {
        pullRequestReviewService = new StubPullRequestReviewService();
        aiReviewService = new StubAiReviewService();
        reviewCommentService = new StubReviewCommentService();
        persistenceService = new StubPersistenceService();
        metrics = new CodeReviewMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        runner = new AsyncCodeReviewRunner(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, metrics);
    }

    @Test
    void testExecuteReviewAsync_PropagatesCorrelationIdAndReviewIdToMdcAndCleansUp() {
        String testCorrelationId = "test-corr-id-999";
        Long reviewId = 555L;

        ReviewInput sampleInput = new ReviewInput(
                100L, "hello-world", "octocat/hello-world", "https://github.com/octocat/hello-world", "main",
                200L, 42L, "PR Title", "PR Body", "open",
                "https://github.com/octocat/hello-world/pull/42", "octocat", "sha999", "main",
                Instant.now(), Instant.now(), List.of(new ReviewFileInput("Main.java", "modified", 5, 1, 6, "@@ -1 +1 @@", null))
        );
        ReviewFinding finding1 = new ReviewFinding("Main.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        ReviewResult sampleResult = new ReviewResult("Summary text", List.of(finding1));
        GithubReviewCommentResponse comment1 = new GithubReviewCommentResponse(1L, "body1", "Main.java", 10, "sha999", "url1", Instant.now());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(sampleResult);
        reviewCommentService.setCommentResponses(List.of(comment1));

        final String[] capturedMdcCorrId = new String[1];
        final String[] capturedMdcReviewId = new String[1];

        pullRequestReviewService.setOnExecuteCallback(() -> {
            capturedMdcCorrId[0] = MDC.get("correlationId");
            capturedMdcReviewId[0] = MDC.get("reviewId");
        });

        runner.executeReviewAsync(reviewId, 12345L, "octocat", "hello-world", 42L, testCorrelationId);

        assertThat(capturedMdcCorrId[0]).isEqualTo(testCorrelationId);
        assertThat(capturedMdcReviewId[0]).isEqualTo(String.valueOf(reviewId));
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("reviewId")).isNull();

        assertThat(metrics.getCompletedCounter().count()).isEqualTo(1.0);
        assertThat(metrics.getFindingsCounter().count()).isEqualTo(1.0);
        assertThat(metrics.getCommentsCounter().count()).isEqualTo(1.0);
    }

    // --- Helper Stubs ---

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        private ReviewInput reviewInput;
        private Runnable onExecuteCallback;

        public StubPullRequestReviewService() { super(null, null, null, null); }

        public void setReviewInput(ReviewInput reviewInput) { this.reviewInput = reviewInput; }
        public void setOnExecuteCallback(Runnable callback) { this.onExecuteCallback = callback; }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            if (onExecuteCallback != null) {
                onExecuteCallback.run();
            }
            return reviewInput;
        }
    }

    private static class StubAiReviewService extends AiReviewService {
        private ReviewResult reviewResult;

        public StubAiReviewService() { super(null); }

        public void setReviewResult(ReviewResult reviewResult) { this.reviewResult = reviewResult; }

        @Override
        public ReviewResult review(ReviewInput input) { return reviewResult; }
    }

    private static class StubReviewCommentService extends GithubReviewCommentService {
        private List<GithubReviewCommentResponse> commentResponses = List.of();

        public StubReviewCommentService() { super(null); }

        public void setCommentResponses(List<GithubReviewCommentResponse> commentResponses) { this.commentResponses = commentResponses; }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            return commentResponses;
        }
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        private CodeReview entity;

        public StubPersistenceService() { super(null); }

        @Override
        public List<CodeReviewFinding> saveFindings(Long reviewId, List<ReviewFinding> findings) {
            return List.of();
        }

        @Override
        public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
            this.entity = new CodeReview(12345L, "octocat", "hello-world", 42);
            this.entity.setId(reviewId);
            this.entity.setStatus(CodeReviewStatus.COMPLETED);
            return entity;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            this.entity = new CodeReview(12345L, "octocat", "hello-world", 42);
            this.entity.setId(reviewId);
            this.entity.setStatus(CodeReviewStatus.FAILED);
            return entity;
        }
    }
}
