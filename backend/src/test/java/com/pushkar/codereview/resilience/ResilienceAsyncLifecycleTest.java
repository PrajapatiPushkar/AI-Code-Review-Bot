package com.pushkar.codereview.resilience;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.AsyncCodeReviewRunner;
import com.pushkar.codereview.github.review.GithubPullRequestReviewService;
import com.pushkar.codereview.github.review.GithubReviewCommentService;
import com.pushkar.codereview.github.review.ai.AiReviewEngine;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceAsyncLifecycleTest {

    private StubPersistenceService persistenceService;
    private CodeReviewMetrics metrics;
    private StubPullRequestReviewService pullRequestReviewService;
    private StubAiReviewEngine aiReviewEngine;
    private AiReviewService aiReviewService;
    private StubReviewCommentService reviewCommentService;
    private AsyncCodeReviewRunner runner;

    @BeforeEach
    void setUp() {
        persistenceService = new StubPersistenceService();
        metrics = new CodeReviewMetrics(new SimpleMeterRegistry());
        pullRequestReviewService = new StubPullRequestReviewService();
        aiReviewEngine = new StubAiReviewEngine();
        aiReviewService = new AiReviewService(aiReviewEngine);
        reviewCommentService = new StubReviewCommentService();

        runner = new AsyncCodeReviewRunner(
                pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, metrics
        );
    }

    @Test
    void testAsyncReviewExecution_SuccessLifecycle() {
        CodeReview review = persistenceService.createInProgressReview(1L, "octocat", "hello-world", 42, null, "sha123");

        runner.executeReviewAsync(review.getId(), 1L, "octocat", "hello-world", 42, "corr-123");

        CodeReview updated = persistenceService.getReview(review.getId());
        assertThat(updated.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
        assertThat(updated.getReviewSummary()).isEqualTo("Clean code");
        assertThat(metrics.getCompletedCounter().count()).isEqualTo(1.0);
    }

    @Test
    void testAsyncReviewExecution_FailureLifecycle_MarksFailed() {
        CodeReview review = persistenceService.createInProgressReview(1L, "octocat", "hello-world", 42, null, "sha123");
        aiReviewEngine.setShouldFail(true);

        runner.executeReviewAsync(review.getId(), 1L, "octocat", "hello-world", 42, "corr-123");

        CodeReview updated = persistenceService.getReview(review.getId());
        assertThat(updated.getStatus()).isEqualTo(CodeReviewStatus.FAILED);
        assertThat(updated.getReviewSummary()).contains("External service error");
        assertThat(metrics.getFailedCounter().count()).isEqualTo(1.0);
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        private final List<CodeReview> reviews = new ArrayList<>();
        private long idCounter = 1L;

        public StubPersistenceService() { super(null); }

        @Override
        public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber, com.pushkar.codereview.user.User user, String commitSha) {
            CodeReview review = new CodeReview(installationId, owner, repositoryName, pullRequestNumber, user, commitSha);
            review.setId(idCounter++);
            review.setStatus(CodeReviewStatus.IN_PROGRESS);
            reviews.add(review);
            return review;
        }

        @Override
        public CodeReview markCompleted(Long reviewId, String summary, int totalFindings, int postedCommentsCount) {
            CodeReview review = getReview(reviewId);
            if (review != null) {
                review.setStatus(CodeReviewStatus.COMPLETED);
                review.setReviewSummary(summary);
                review.setTotalFindings(totalFindings);
                review.setPostedCommentsCount(postedCommentsCount);
            }
            return review;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            CodeReview review = getReview(reviewId);
            if (review != null) {
                review.setStatus(CodeReviewStatus.FAILED);
                review.setReviewSummary(errorMessage);
            }
            return review;
        }

        public CodeReview getReview(Long id) {
            return reviews.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        }
    }

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        public StubPullRequestReviewService() { super(null, null, null); }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            ReviewInput input = new ReviewInput();
            input.setHeadBranch("sha123");
            return input;
        }
    }

    private static class StubAiReviewEngine implements AiReviewEngine {
        private boolean shouldFail = false;

        public void setShouldFail(boolean shouldFail) { this.shouldFail = shouldFail; }

        @Override
        public ReviewResult review(ReviewInput input) {
            if (shouldFail) {
                throw new RuntimeException("External service error");
            }
            return new ReviewResult("Clean code", List.of());
        }
    }

    private static class StubReviewCommentService extends GithubReviewCommentService {
        public StubReviewCommentService() { super(null); }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            return List.of();
        }
    }
}
