package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubPullRequestCodeReviewService {

    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiReviewService aiReviewService;
    private final GithubReviewCommentService reviewCommentService;
    private final CodeReviewPersistenceService persistenceService;
    private final CurrentUserService currentUserService;
    private final GithubInstallationRepository githubInstallationRepository;

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, null, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, currentUserService, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService,
                                               GithubInstallationRepository githubInstallationRepository) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
        this.persistenceService = persistenceService;
        this.currentUserService = currentUserService;
        this.githubInstallationRepository = githubInstallationRepository;
    }

    public CodeReviewExecutionResult executeCodeReview(Long installationId,
                                                         String owner,
                                                         String repository,
                                                         int pullRequestNumber) {
        return executeCodeReview(installationId, owner, repository, (long) pullRequestNumber);
    }

    public CodeReviewExecutionResult executeCodeReview(Long requestedInstallationId,
                                                         String owner,
                                                         String repository,
                                                         long pullRequestNumber) {
        validateInputs(requestedInstallationId, owner, repository, pullRequestNumber);

        User currentUser = null;
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            currentUser = currentUserService.getCurrentUser();
        }

        Long actualGithubInstallationId = requestedInstallationId;

        if (githubInstallationRepository != null) {
            GithubInstallation installation = githubInstallationRepository.findById(requestedInstallationId)
                    .or(() -> githubInstallationRepository.findByGithubInstallationId(requestedInstallationId))
                    .orElseThrow(() -> new ResourceNotFoundException("GitHub installation not found with ID: " + requestedInstallationId));

            if (currentUserService != null && currentUserService.isAuthenticated()) {
                if (!currentUserService.hasRole("ADMIN")) {
                    Long currentUserId = currentUserService.getCurrentUserId();
                    if (installation.getUser() != null && !installation.getUser().getId().equals(currentUserId)) {
                        throw new AccessDeniedException("You do not have permission to access this installation");
                    }
                }
            }

            if (!installation.isVerified()) {
                throw new GithubInstallationVerificationException("GitHub installation ID " + requestedInstallationId + " is not verified");
            }

            actualGithubInstallationId = installation.getGithubInstallationId();
        }

        CodeReview reviewRecord = null;
        if (persistenceService != null) {
            reviewRecord = persistenceService.createInProgressReview(actualGithubInstallationId, owner, repository, (int) pullRequestNumber, currentUser);
        }

        try {
            ReviewInput reviewInput = pullRequestReviewService.getReviewInput(actualGithubInstallationId, owner, repository, pullRequestNumber);
            ReviewResult reviewResult = aiReviewService.review(reviewInput);

            String commitId = (reviewInput != null && reviewInput.getHeadBranch() != null && !reviewInput.getHeadBranch().isBlank())
                    ? reviewInput.getHeadBranch()
                    : "HEAD";

            List<GithubReviewCommentResponse> postedComments = reviewCommentService.postReviewComments(
                    actualGithubInstallationId, owner, repository, pullRequestNumber, commitId, reviewResult
            );

            String summary = (reviewResult != null) ? reviewResult.getSummary() : "";
            int totalFindings = (reviewResult != null && reviewResult.getFindings() != null) ? reviewResult.getFindings().size() : 0;
            int postedCommentsCount = (postedComments != null) ? postedComments.size() : 0;

            Long reviewId = null;
            if (persistenceService != null && reviewRecord != null && reviewRecord.getId() != null) {
                persistenceService.markCompleted(reviewRecord.getId(), summary, totalFindings, postedCommentsCount);
                reviewId = reviewRecord.getId();
            }

            return new CodeReviewExecutionResult(
                    reviewId,
                    actualGithubInstallationId,
                    owner,
                    repository,
                    pullRequestNumber,
                    "COMPLETED",
                    summary,
                    totalFindings,
                    postedCommentsCount
            );

        } catch (Exception ex) {
            if (persistenceService != null && reviewRecord != null && reviewRecord.getId() != null) {
                persistenceService.markFailed(reviewRecord.getId(), ex.getMessage());
            }
            throw ex;
        }
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
