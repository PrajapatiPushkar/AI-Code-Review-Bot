package com.pushkar.codereview.github.review;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AsyncCodeReviewRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncCodeReviewRunner.class);

    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiReviewService aiReviewService;
    private final GithubReviewCommentService reviewCommentService;
    private final CodeReviewPersistenceService persistenceService;
    private final CodeReviewMetrics codeReviewMetrics;

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, null);
    }

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService,
                                  @Autowired(required = false) CodeReviewMetrics codeReviewMetrics) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
        this.persistenceService = persistenceService;
        this.codeReviewMetrics = codeReviewMetrics;
    }

    @Async("taskExecutor")
    public void executeReviewAsync(Long reviewId, Long installationId, String owner, String repository, long pullRequestNumber) {
        executeReviewAsync(reviewId, installationId, owner, repository, pullRequestNumber, MDC.get("correlationId"));
    }

    @Async("taskExecutor")
    public void executeReviewAsync(Long reviewId, Long installationId, String owner, String repository, long pullRequestNumber, String correlationId) {
        String activeCorrelationId = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : UUID.randomUUID().toString();

        MDC.put("correlationId", activeCorrelationId);
        if (reviewId != null) {
            MDC.put("reviewId", String.valueOf(reviewId));
        }

        long startTime = System.currentTimeMillis();
        log.info("Starting async code review execution: reviewId={}, repository={}/{}, pullRequestNumber={}",
                reviewId, owner, repository, pullRequestNumber);

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

            long duration = System.currentTimeMillis() - startTime;
            if (codeReviewMetrics != null) {
                codeReviewMetrics.recordFindings(totalFindings);
                codeReviewMetrics.recordCommentsPosted(postedCommentsCount);
                codeReviewMetrics.recordCompleted();
                codeReviewMetrics.recordExecutionTime(duration);
            }

            log.info("Completed async code review execution: reviewId={}, totalFindings={}, postedComments={}, duration={} ms",
                    reviewId, totalFindings, postedCommentsCount, duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            if (codeReviewMetrics != null) {
                codeReviewMetrics.recordFailed();
                codeReviewMetrics.recordExecutionTime(duration);
            }

            log.error("Async code review execution failed: reviewId={}, duration={} ms, error={}",
                    reviewId, duration, ex.getMessage(), ex);

            if (persistenceService != null && reviewId != null) {
                persistenceService.markFailed(reviewId, ex.getMessage());
            }
        } finally {
            MDC.remove("correlationId");
            MDC.remove("reviewId");
        }
    }
}
