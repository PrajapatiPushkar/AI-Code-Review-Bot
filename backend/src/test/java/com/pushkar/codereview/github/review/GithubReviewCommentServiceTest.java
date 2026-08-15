package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.github.client.GithubPullRequestReviewCommentClient;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentRequest;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubReviewCommentServiceTest {

    private static final Long INSTALLATION_ID = 12345L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final long PR_NUMBER = 42L;
    private static final String COMMIT_ID = "sha123456";

    private StubCommentClient stubClient;
    private GithubReviewCommentService commentService;

    @BeforeEach
    void setUp() {
        stubClient = new StubCommentClient();
        commentService = new GithubReviewCommentService(stubClient);
    }

    @Test
    void testPostReviewComments_SuccessWithEligibleFindings() {
        ReviewFinding finding1 = new ReviewFinding(
                "src/Main.java", 15, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG,
                "Null pointer exception possible", "Add null check"
        );
        ReviewResult result = new ReviewResult("Summary", List.of(finding1));

        List<GithubReviewCommentResponse> comments = commentService.postReviewComments(
                INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, result
        );

        assertThat(comments).hasSize(1);
        assertThat(stubClient.getRequests()).hasSize(1);

        GithubReviewCommentRequest request = stubClient.getRequests().get(0);
        assertThat(request.getPath()).isEqualTo("src/Main.java");
        assertThat(request.getLine()).isEqualTo(15);
        assertThat(request.getCommitId()).isEqualTo(COMMIT_ID);
        assertThat(request.getBody()).contains("**[AI Review - HIGH - BUG]**");
        assertThat(request.getBody()).contains("Null pointer exception possible");
        assertThat(request.getBody()).contains("💡 **Suggestion:**\nAdd null check");
    }

    @Test
    void testPostReviewComments_IneligibleFindingsSkipped() {
        ReviewFinding noLineFinding = new ReviewFinding(
                "src/Main.java", null, ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.MAINTAINABILITY,
                "General file architecture issue", "Refactor file"
        );
        ReviewFinding negativeLineFinding = new ReviewFinding(
                "src/Main.java", -1, ReviewFindingSeverity.LOW, ReviewFindingCategory.CODE_STYLE,
                "Bad line", "Fix line"
        );
        ReviewFinding noFileFinding = new ReviewFinding(
                "", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.SECURITY,
                "Missing file path", "Fix path"
        );

        ReviewResult result = new ReviewResult("Summary", List.of(noLineFinding, negativeLineFinding, noFileFinding));

        List<GithubReviewCommentResponse> comments = commentService.postReviewComments(
                INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, result
        );

        assertThat(comments).isEmpty();
        assertThat(stubClient.getRequests()).isEmpty();
    }

    @Test
    void testPostReviewComments_MultipleFindingsMix() {
        ReviewFinding eligible1 = new ReviewFinding(
                "src/A.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug in A", "Fix A"
        );
        ReviewFinding ineligibleNoLine = new ReviewFinding(
                "src/B.java", null, ReviewFindingSeverity.INFO, ReviewFindingCategory.OTHER, "General B issue", "Fix B"
        );
        ReviewFinding eligible2 = new ReviewFinding(
                "src/C.java", 42, ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY, "Vulnerability in C", "Fix C"
        );

        ReviewResult result = new ReviewResult("Summary", List.of(eligible1, ineligibleNoLine, eligible2));

        List<GithubReviewCommentResponse> comments = commentService.postReviewComments(
                INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, result
        );

        assertThat(comments).hasSize(2);
        assertThat(stubClient.getRequests()).hasSize(2);
        assertThat(stubClient.getRequests().get(0).getPath()).isEqualTo("src/A.java");
        assertThat(stubClient.getRequests().get(1).getPath()).isEqualTo("src/C.java");
    }

    @Test
    void testPostReviewComments_EmptyFindings() {
        ReviewResult result = new ReviewResult("Summary", Collections.emptyList());

        List<GithubReviewCommentResponse> comments = commentService.postReviewComments(
                INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, result
        );

        assertThat(comments).isEmpty();
        assertThat(stubClient.getRequests()).isEmpty();
    }

    @Test
    void testPostReviewComments_InvalidInputs() {
        ReviewResult validResult = new ReviewResult("Summary", Collections.emptyList());

        assertThatThrownBy(() -> commentService.postReviewComments(null, OWNER, REPO, PR_NUMBER, COMMIT_ID, validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(0L, OWNER, REPO, PR_NUMBER, COMMIT_ID, validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, " ", REPO, PR_NUMBER, COMMIT_ID, validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, OWNER, "", PR_NUMBER, COMMIT_ID, validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, OWNER, REPO, 0, COMMIT_ID, validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, " ", validResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPostReviewComments_PropagatesClientException() {
        ReviewFinding finding = new ReviewFinding(
                "src/Main.java", 15, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG,
                "Bug", "Fix"
        );
        ReviewResult result = new ReviewResult("Summary", List.of(finding));

        stubClient.setException(new GithubApiException("GitHub API error", 502));

        assertThatThrownBy(() -> commentService.postReviewComments(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, COMMIT_ID, result))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API error");
    }

    // --- Helper Stub ---

    private static class StubCommentClient extends GithubPullRequestReviewCommentClient {
        private final List<GithubReviewCommentRequest> requests = new ArrayList<>();
        private RuntimeException exception;
        private long idCounter = 100L;

        public StubCommentClient() {
            super((RestClient) null, null);
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public List<GithubReviewCommentRequest> getRequests() {
            return requests;
        }

        @Override
        public GithubReviewCommentResponse createReviewComment(Long installationId, String owner, String repository, long pullRequestNumber, GithubReviewCommentRequest request) {
            if (exception != null) {
                throw exception;
            }
            requests.add(request);
            long id = idCounter++;
            return new GithubReviewCommentResponse(
                    id, request.getBody(), request.getPath(), request.getLine(), request.getCommitId(),
                    "https://github.com/" + owner + "/" + repository + "/pull/" + pullRequestNumber + "#discussion_r" + id,
                    Instant.now()
            );
        }
    }
}
