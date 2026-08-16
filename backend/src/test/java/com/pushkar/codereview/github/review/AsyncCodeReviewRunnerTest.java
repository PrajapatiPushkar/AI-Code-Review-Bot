package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCodeReviewRunnerTest {

    private StubPullRequestReviewService pullRequestReviewService;
    private StubAiReviewService aiReviewService;
    private StubReviewCommentService reviewCommentService;
    private StubPersistenceService persistenceService;
    private AsyncCodeReviewRunner runner;

    private ReviewInput sampleInput;

    @BeforeEach
    void setUp() {
        pullRequestReviewService = new StubPullRequestReviewService();
        aiReviewService = new StubAiReviewService();
        reviewCommentService = new StubReviewCommentService();
        persistenceService = new StubPersistenceService();

        runner = new AsyncCodeReviewRunner(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService);

        sampleInput = new ReviewInput(
                100L, "hello-world", "octocat/hello-world", "https://github.com/octocat/hello-world", "main",
                200L, 42L, "PR Title", "PR Body", "open",
                "https://github.com/octocat/hello-world/pull/42", "octocat", "sha999", "main",
                Instant.now(), Instant.now(), List.of(new ReviewFileInput("Main.java", "modified", 5, 1, 6, "@@ -1 +1 @@", null))
        );
    }

    @Test
    void testExecuteReviewAsync_Success_MarksCompleted() {
        ReviewFinding finding1 = new ReviewFinding("Main.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        ReviewResult sampleResult = new ReviewResult("Summary text", List.of(finding1));

        GithubReviewCommentResponse comment1 = new GithubReviewCommentResponse(1L, "body1", "Main.java", 10, "sha999", "url1", Instant.now());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(sampleResult);
        reviewCommentService.setCommentResponses(List.of(comment1));

        runner.executeReviewAsync(100L, 12345L, "octocat", "hello-world", 42L);

        assertThat(persistenceService.isMarkedCompleted()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
        assertThat(persistenceService.getLastEntity().getReviewSummary()).isEqualTo("Summary text");
        assertThat(persistenceService.getLastEntity().getTotalFindings()).isEqualTo(1);
        assertThat(persistenceService.getLastEntity().getPostedCommentsCount()).isEqualTo(1);
    }

    @Test
    void testExecuteReviewAsync_Failure_MarksFailed() {
        pullRequestReviewService.setException(new ResourceNotFoundException("GitHub PR not found"));

        runner.executeReviewAsync(100L, 12345L, "octocat", "hello-world", 42L);

        assertThat(persistenceService.isMarkedFailed()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.FAILED);
        assertThat(persistenceService.getLastEntity().getReviewSummary()).contains("FAILED: GitHub PR not found");
    }

    // --- Helper Stubs ---

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        private ReviewInput reviewInput;
        private RuntimeException exception;

        public StubPullRequestReviewService() { super(null, null, null, null); }

        public void setReviewInput(ReviewInput reviewInput) { this.reviewInput = reviewInput; }
        public void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            if (exception != null) throw exception;
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
        private List<GithubReviewCommentResponse> commentResponses = new ArrayList<>();

        public StubReviewCommentService() { super(null); }

        public void setCommentResponses(List<GithubReviewCommentResponse> commentResponses) { this.commentResponses = commentResponses; }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            return commentResponses;
        }
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        private CodeReview entity;
        private boolean markedCompleted = false;
        private boolean markedFailed = false;

        public StubPersistenceService() { super(null); }

        public boolean isMarkedCompleted() { return markedCompleted; }
        public boolean isMarkedFailed() { return markedFailed; }
        public CodeReview getLastEntity() { return entity; }

        @Override
        public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
            this.markedCompleted = true;
            this.entity = new CodeReview(12345L, "octocat", "hello-world", 42);
            this.entity.setId(reviewId);
            this.entity.setStatus(CodeReviewStatus.COMPLETED);
            this.entity.setReviewSummary(reviewSummary);
            this.entity.setTotalFindings(totalFindings);
            this.entity.setPostedCommentsCount(postedCommentsCount);
            this.entity.setCompletedAt(Instant.now());
            return entity;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            this.markedFailed = true;
            this.entity = new CodeReview(12345L, "octocat", "hello-world", 42);
            this.entity.setId(reviewId);
            this.entity.setStatus(CodeReviewStatus.FAILED);
            this.entity.setReviewSummary("FAILED: " + errorMessage);
            this.entity.setCompletedAt(Instant.now());
            return entity;
        }
    }
}
