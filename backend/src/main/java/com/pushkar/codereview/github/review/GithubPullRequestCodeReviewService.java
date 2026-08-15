package com.pushkar.codereview.github.review;

import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubPullRequestCodeReviewService {

    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiReviewService aiReviewService;
    private final GithubReviewCommentService reviewCommentService;

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
    }

    public CodeReviewExecutionResult executeCodeReview(Long installationId,
                                                         String owner,
                                                         String repository,
                                                         int pullRequestNumber) {
        return executeCodeReview(installationId, owner, repository, (long) pullRequestNumber);
    }

    public CodeReviewExecutionResult executeCodeReview(Long installationId,
                                                         String owner,
                                                         String repository,
                                                         long pullRequestNumber) {
        validateInputs(installationId, owner, repository, pullRequestNumber);

        // Step 1: Fetch review input context from GitHub
        ReviewInput reviewInput = pullRequestReviewService.getReviewInput(installationId, owner, repository, pullRequestNumber);

        // Step 2: Execute AI code review
        ReviewResult reviewResult = aiReviewService.review(reviewInput);

        // Step 3: Post inline review comments on GitHub PR
        String commitId = (reviewInput != null && reviewInput.getHeadBranch() != null && !reviewInput.getHeadBranch().isBlank())
                ? reviewInput.getHeadBranch()
                : "HEAD";

        List<GithubReviewCommentResponse> postedComments = reviewCommentService.postReviewComments(
                installationId, owner, repository, pullRequestNumber, commitId, reviewResult
        );

        // Step 4: Build execution summary result
        String repoFullName = (reviewInput != null && reviewInput.getRepositoryFullName() != null)
                ? reviewInput.getRepositoryFullName()
                : owner + "/" + repository;
        String summary = (reviewResult != null) ? reviewResult.getSummary() : "";
        int totalFindings = (reviewResult != null && reviewResult.getFindings() != null) ? reviewResult.getFindings().size() : 0;
        int postedCommentsCount = (postedComments != null) ? postedComments.size() : 0;

        return new CodeReviewExecutionResult(repoFullName, pullRequestNumber, summary, totalFindings, postedCommentsCount);
    }

    private void validateInputs(Long installationId, String owner, String repository, long pullRequestNumber) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
        if (pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be a positive number");
        }
    }
}
