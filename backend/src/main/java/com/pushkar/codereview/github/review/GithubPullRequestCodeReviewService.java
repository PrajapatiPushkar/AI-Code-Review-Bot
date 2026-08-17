package com.pushkar.codereview.github.review;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GithubPullRequestCodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(GithubPullRequestCodeReviewService.class);

    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiReviewService aiReviewService;
    private final GithubReviewCommentService reviewCommentService;
    private final CodeReviewPersistenceService persistenceService;
    private final CurrentUserService currentUserService;
    private final GithubInstallationRepository githubInstallationRepository;
    private final AsyncCodeReviewRunner asyncCodeReviewRunner;
    private final CodeReviewMetrics codeReviewMetrics;

    private final ConcurrentHashMap<String, Object> reviewLocks = new ConcurrentHashMap<>();

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, null, null, null, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, currentUserService, null, null, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService,
                                               GithubInstallationRepository githubInstallationRepository) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, currentUserService, githubInstallationRepository, null, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService,
                                               GithubInstallationRepository githubInstallationRepository,
                                               AsyncCodeReviewRunner asyncCodeReviewRunner) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, currentUserService, githubInstallationRepository, asyncCodeReviewRunner, null);
    }

    public GithubPullRequestCodeReviewService(GithubPullRequestReviewService pullRequestReviewService,
                                               AiReviewService aiReviewService,
                                               GithubReviewCommentService reviewCommentService,
                                               CodeReviewPersistenceService persistenceService,
                                               CurrentUserService currentUserService,
                                               GithubInstallationRepository githubInstallationRepository,
                                               AsyncCodeReviewRunner asyncCodeReviewRunner,
                                               @Autowired(required = false) CodeReviewMetrics codeReviewMetrics) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
        this.persistenceService = persistenceService;
        this.currentUserService = currentUserService;
        this.githubInstallationRepository = githubInstallationRepository;
        this.asyncCodeReviewRunner = asyncCodeReviewRunner;
        this.codeReviewMetrics = codeReviewMetrics;
    }

    public CodeReviewExecutionResult executeCodeReview(Long installationId,
                                                         String owner,
                                                         String repository,
                                                         int pullRequestNumber) {
        return executeCodeReview(installationId, owner, repository, (long) pullRequestNumber, null);
    }

    public CodeReviewExecutionResult executeCodeReview(Long requestedInstallationId,
                                                         String owner,
                                                         String repository,
                                                         long pullRequestNumber) {
        return executeCodeReview(requestedInstallationId, owner, repository, pullRequestNumber, null);
    }

    public CodeReviewExecutionResult executeCodeReview(Long requestedInstallationId,
                                                         String owner,
                                                         String repository,
                                                         long pullRequestNumber,
                                                         String commitSha) {
        log.info("Code review request received for installationId={}, owner={}, repo={}, prNumber={}",
                requestedInstallationId, owner, repository, pullRequestNumber);

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

        String resolvedCommitSha = commitSha;
        if ((resolvedCommitSha == null || resolvedCommitSha.isBlank()) && pullRequestReviewService != null) {
            try {
                PullRequestReviewContext context = pullRequestReviewService.getReviewContext(actualGithubInstallationId, owner, repository, pullRequestNumber);
                if (context != null && context.getPullRequest() != null && context.getPullRequest().getHead() != null) {
                    resolvedCommitSha = context.getPullRequest().getHead().getSha();
                }
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                // If fetching PR context fails, fall back to null resolvedCommitSha
            }
        }

        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String lockKey = String.format("%s:%s:%s:%s:%s:%s",
                userId != null ? userId : "anonymous",
                actualGithubInstallationId,
                owner.toLowerCase(),
                repository.toLowerCase(),
                pullRequestNumber,
                resolvedCommitSha != null ? resolvedCommitSha : "");

        Object lock = reviewLocks.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            try {
                if (persistenceService != null) {
                    Optional<CodeReview> existingReviewOpt = persistenceService.findDuplicateReview(
                            userId, actualGithubInstallationId, owner, repository, (int) pullRequestNumber, resolvedCommitSha
                    );

                    if (existingReviewOpt.isPresent()) {
                        CodeReview existing = existingReviewOpt.get();
                        String existingStatus = existing.getStatus() != null ? existing.getStatus().name() : "IN_PROGRESS";
                        String existingSummary = existing.getReviewSummary() != null ? existing.getReviewSummary() : "";
                        int totalFindings = existing.getTotalFindings() != null ? existing.getTotalFindings() : 0;
                        int postedComments = existing.getPostedCommentsCount() != null ? existing.getPostedCommentsCount() : 0;

                        if (codeReviewMetrics != null) {
                            codeReviewMetrics.recordSubmission(true);
                        }

                        log.info("Duplicate code review request detected: reviewId={}, owner={}, repo={}, prNumber={}",
                                existing.getId(), owner, repository, pullRequestNumber);

                        return new CodeReviewExecutionResult(
                                existing.getId(),
                                existing.getInstallationId(),
                                existing.getOwner(),
                                existing.getRepository(),
                                existing.getPullRequestNumber() != null ? existing.getPullRequestNumber().longValue() : pullRequestNumber,
                                existingStatus,
                                existingSummary,
                                totalFindings,
                                postedComments,
                                false,
                                existing.getCommitSha() != null ? existing.getCommitSha() : resolvedCommitSha
                        );
                    }
                }

                CodeReview reviewRecord = null;
                if (persistenceService != null) {
                    reviewRecord = persistenceService.createInProgressReview(actualGithubInstallationId, owner, repository, (int) pullRequestNumber, currentUser, resolvedCommitSha);
                }

                Long reviewId = (reviewRecord != null) ? reviewRecord.getId() : null;

                if (codeReviewMetrics != null) {
                    codeReviewMetrics.recordSubmission(false);
                }

                log.info("Initiating code review [reviewId={}] for repository={}/{}, prNumber={}",
                        reviewId, owner, repository, pullRequestNumber);

                if (asyncCodeReviewRunner != null) {
                    asyncCodeReviewRunner.executeReviewAsync(reviewId, actualGithubInstallationId, owner, repository, pullRequestNumber);
                }

                return new CodeReviewExecutionResult(
                        reviewId,
                        actualGithubInstallationId,
                        owner,
                        repository,
                        pullRequestNumber,
                        "IN_PROGRESS",
                        "",
                        0,
                        0,
                        true,
                        resolvedCommitSha
                );
            } finally {
                reviewLocks.remove(lockKey, lock);
            }
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
