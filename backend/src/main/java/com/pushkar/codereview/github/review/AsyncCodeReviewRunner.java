package com.pushkar.codereview.github.review;

import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsyncCodeReviewRunner {

    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiReviewService aiReviewService;
    private final GithubReviewCommentService reviewCommentService;
    private final CodeReviewPersistenceService persistenceService;

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
        this.persistenceService = persistenceService;
    }

    @Async("taskExecutor")
    public void executeReviewAsync(Long reviewId, Long installationId, String owner, String repository, long pullRequestNumber) {
        try {
            ReviewInput reviewInput = pullRequestReviewService.getReviewInput(installationId, owner, repository, pullRequestNumber);
            ReviewResult reviewResult = aiReviewService.review(reviewInput);

            String commitId = (reviewInput != null && reviewInput.getHeadBranch() != null && !reviewInput.getHeadBranch().isBlank())
                    ? reviewInput.getHeadBranch()
                    : "HEAD";

            List<GithubReviewCommentResponse> postedComments = reviewCommentService.postReviewComments(
                    installationId, owner, repository, pullRequestNumber, commitId, reviewResult
            );

            if (persistenceService != null && reviewId != null && reviewResult != null && reviewResult.getFindings() != null) {
                persistenceService.saveFindings(reviewId, reviewResult.getFindings());
            }

            String summary = (reviewResult != null) ? reviewResult.getSummary() : "";
            int totalFindings = (reviewResult != null && reviewResult.getFindings() != null) ? reviewResult.getFindings().size() : 0;
            int postedCommentsCount = (postedComments != null) ? postedComments.size() : 0;

            if (persistenceService != null && reviewId != null) {
                persistenceService.markCompleted(reviewId, summary, totalFindings, postedCommentsCount);
            }
        } catch (Exception ex) {
            if (persistenceService != null && reviewId != null) {
                persistenceService.markFailed(reviewId, ex.getMessage());
            }
        }
    }
}
