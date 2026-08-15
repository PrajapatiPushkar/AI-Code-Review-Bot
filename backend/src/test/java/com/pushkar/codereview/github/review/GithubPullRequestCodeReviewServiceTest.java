package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubPullRequestCodeReviewServiceTest {

    private static final Long INSTALLATION_ID = 12345L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final int PR_NUMBER = 42;
    private static final String COMMIT_SHA = "sha999888";

    private StubPullRequestReviewService pullRequestReviewService;
    private StubAiReviewService aiReviewService;
    private StubReviewCommentService reviewCommentService;
    private StubPersistenceService persistenceService;
    private GithubPullRequestCodeReviewService codeReviewService;

    private ReviewInput sampleInput;

    @BeforeEach
    void setUp() {
        pullRequestReviewService = new StubPullRequestReviewService();
        aiReviewService = new StubAiReviewService();
        reviewCommentService = new StubReviewCommentService();
        persistenceService = new StubPersistenceService();
        codeReviewService = new GithubPullRequestCodeReviewService(
                pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService
        );

        sampleInput = new ReviewInput(
                100L, REPO, OWNER + "/" + REPO, "https://github.com/octocat/hello-world", "main",
                200L, (long) PR_NUMBER, "PR Title", "PR Body", "open",
                "https://github.com/octocat/hello-world/pull/42", OWNER, COMMIT_SHA, "main",
                Instant.now(), Instant.now(), List.of(new ReviewFileInput("Main.java", "modified", 5, 1, 6, "@@ -1 +1 @@", null))
        );
    }

    @Test
    void testExecuteCodeReview_SuccessFlowWithPersistence() {
        ReviewFinding finding1 = new ReviewFinding("Main.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        ReviewFinding finding2 = new ReviewFinding("Main.java", 20, ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.PERFORMANCE, "Perf msg", "Fix perf");
        ReviewResult sampleResult = new ReviewResult("Review summary text", List.of(finding1, finding2));

        GithubReviewCommentResponse comment1 = new GithubReviewCommentResponse(1L, "body1", "Main.java", 10, COMMIT_SHA, "url1", Instant.now());
        GithubReviewCommentResponse comment2 = new GithubReviewCommentResponse(2L, "body2", "Main.java", 20, COMMIT_SHA, "url2", Instant.now());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(sampleResult);
        reviewCommentService.setCommentResponses(List.of(comment1, comment2));

        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getRepository()).isEqualTo("octocat/hello-world");
        assertThat(result.getPullRequestNumber()).isEqualTo((long) PR_NUMBER);
        assertThat(result.getReviewSummary()).isEqualTo("Review summary text");
        assertThat(result.getTotalFindings()).isEqualTo(2);
        assertThat(result.getPostedCommentsCount()).isEqualTo(2);

        // Persistence verifications
        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
    }

    @Test
    void testExecuteCodeReview_GitHubRetrievalFailureMarksFailed() {
        pullRequestReviewService.setException(new ResourceNotFoundException("GitHub PR not found"));

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub PR not found");

        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.FAILED);
    }

    @Test
    void testExecuteCodeReview_AiReviewFailureMarksFailed() {
        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setException(new RuntimeException("AI engine quota exceeded"));

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI engine quota exceeded");

        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.FAILED);
    }

    @Test
    void testExecuteCodeReview_CommentPostingFailureMarksFailed() {
        ReviewResult sampleResult = new ReviewResult("Summary", Collections.emptyList());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(sampleResult);
        reviewCommentService.setException(new GithubApiException("GitHub API write error", 502));

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API write error");

        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isFalse();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.FAILED);
    }

    @Test
    void testExecuteCodeReview_ZeroFindings() {
        ReviewResult zeroResult = new ReviewResult("No issues found", Collections.emptyList());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(zeroResult);
        reviewCommentService.setCommentResponses(Collections.emptyList());

        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getTotalFindings()).isEqualTo(0);
        assertThat(result.getPostedCommentsCount()).isEqualTo(0);
        assertThat(result.getReviewSummary()).isEqualTo("No issues found");
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
    }

    @Test
    void testExecuteCodeReview_InvalidParameters() {
        // Invalid installationId
        assertThatThrownBy(() -> codeReviewService.executeCodeReview(null, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(persistenceService.isCreatedInProgress()).isFalse();
    }

    // --- Helper Stubs ---

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        private ReviewInput reviewInput;
        private RuntimeException exception;

        public StubPullRequestReviewService() {
            super(null, null, null, null);
        }

        public void setReviewInput(ReviewInput reviewInput) {
            this.reviewInput = reviewInput;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            if (exception != null) {
                throw exception;
            }
            return reviewInput;
        }
    }

    private static class StubAiReviewService extends AiReviewService {
        private ReviewResult reviewResult;
        private RuntimeException exception;

        public StubAiReviewService() {
            super(null);
        }

        public void setReviewResult(ReviewResult reviewResult) {
            this.reviewResult = reviewResult;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public ReviewResult review(ReviewInput input) {
            if (exception != null) {
                throw exception;
            }
            return reviewResult;
        }
    }

    private static class StubReviewCommentService extends GithubReviewCommentService {
        private List<GithubReviewCommentResponse> commentResponses = new ArrayList<>();
        private RuntimeException exception;

        public StubReviewCommentService() {
            super(null);
        }

        public void setCommentResponses(List<GithubReviewCommentResponse> commentResponses) {
            this.commentResponses = commentResponses;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            if (exception != null) {
                throw exception;
            }
            return commentResponses;
        }
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        private CodeReview entity;
        private boolean createdInProgress = false;
        private boolean markedCompleted = false;
        private boolean markedFailed = false;

        public StubPersistenceService() {
            super(null);
        }

        public boolean isCreatedInProgress() {
            return createdInProgress;
        }

        public boolean isMarkedCompleted() {
            return markedCompleted;
        }

        public boolean isMarkedFailed() {
            return markedFailed;
        }

        public CodeReview getLastEntity() {
            return entity;
        }

        @Override
        public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber) {
            this.createdInProgress = true;
            this.entity = new CodeReview(installationId, owner, repositoryName, pullRequestNumber);
            this.entity.setId(100L);
            this.entity.setStatus(CodeReviewStatus.IN_PROGRESS);
            this.entity.setCreatedAt(Instant.now());
            return entity;
        }

        @Override
        public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
            this.markedCompleted = true;
            if (entity != null) {
                entity.setStatus(CodeReviewStatus.COMPLETED);
                entity.setReviewSummary(reviewSummary);
                entity.setTotalFindings(totalFindings);
                entity.setPostedCommentsCount(postedCommentsCount);
                entity.setCompletedAt(Instant.now());
            }
            return entity;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            this.markedFailed = true;
            if (entity != null) {
                entity.setStatus(CodeReviewStatus.FAILED);
                entity.setReviewSummary("FAILED: " + errorMessage);
                entity.setCompletedAt(Instant.now());
            }
            return entity;
        }
    }
}
