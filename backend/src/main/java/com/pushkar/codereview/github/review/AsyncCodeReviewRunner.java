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
    private final com.pushkar.codereview.github.review.rule.DeterministicRuleEngine deterministicRuleEngine;
    private final com.pushkar.codereview.github.review.rule.ReviewFindingMerger findingMerger;
    private final com.pushkar.codereview.qualitygate.QualityGateService qualityGateService;
    private final com.pushkar.codereview.policy.RepositoryPolicyService repositoryPolicyService;

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, null, null, null, null, null);
    }

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService,
                                  CodeReviewMetrics codeReviewMetrics) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, codeReviewMetrics, null, null, null, null);
    }

    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService,
                                  CodeReviewMetrics codeReviewMetrics,
                                  com.pushkar.codereview.github.review.rule.DeterministicRuleEngine deterministicRuleEngine,
                                  com.pushkar.codereview.github.review.rule.ReviewFindingMerger findingMerger) {
        this(pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, codeReviewMetrics, deterministicRuleEngine, findingMerger, null, null);
    }

    @Autowired
    public AsyncCodeReviewRunner(GithubPullRequestReviewService pullRequestReviewService,
                                  AiReviewService aiReviewService,
                                  GithubReviewCommentService reviewCommentService,
                                  CodeReviewPersistenceService persistenceService,
                                  @Autowired(required = false) CodeReviewMetrics codeReviewMetrics,
                                  @Autowired(required = false) com.pushkar.codereview.github.review.rule.DeterministicRuleEngine deterministicRuleEngine,
                                  @Autowired(required = false) com.pushkar.codereview.github.review.rule.ReviewFindingMerger findingMerger,
                                  @Autowired(required = false) com.pushkar.codereview.qualitygate.QualityGateService qualityGateService,
                                  @Autowired(required = false) com.pushkar.codereview.policy.RepositoryPolicyService repositoryPolicyService) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiReviewService = aiReviewService;
        this.reviewCommentService = reviewCommentService;
        this.persistenceService = persistenceService;
        this.codeReviewMetrics = codeReviewMetrics;
        this.deterministicRuleEngine = deterministicRuleEngine;
        this.findingMerger = findingMerger != null ? findingMerger : new com.pushkar.codereview.github.review.rule.ReviewFindingMerger();
        this.qualityGateService = qualityGateService;
        this.repositoryPolicyService = repositoryPolicyService;
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
        if (codeReviewMetrics != null) {
            codeReviewMetrics.incrementInProgress();
        }
        log.info("Starting async code review execution: reviewId={}, repository={}/{}, pullRequestNumber={}",
                reviewId, owner, repository, pullRequestNumber);

        try {
            ReviewInput reviewInput = pullRequestReviewService.getReviewInput(installationId, owner, repository, pullRequestNumber);
            ReviewResult aiReviewResult = aiReviewService.review(reviewInput);

            String commitId = null;
            if (persistenceService != null && reviewId != null) {
                commitId = persistenceService.findById(reviewId)
                        .map(com.pushkar.codereview.github.review.persistence.CodeReview::getCommitSha)
                        .orElse(null);
            }
            if (commitId == null || commitId.isBlank()) {
                commitId = (reviewInput != null && reviewInput.getHeadBranch() != null && !reviewInput.getHeadBranch().isBlank())
                        ? reviewInput.getHeadBranch()
                        : "HEAD";
            }

            // Resolve effective repository policy
            com.pushkar.codereview.policy.RepositoryReviewPolicy effectivePolicy = null;
            if (repositoryPolicyService != null) {
                try {
                    effectivePolicy = repositoryPolicyService.resolveEffectivePolicy(owner, repository);
                } catch (Exception policyEx) {
                    log.warn("Failed resolving review policy for repository={}/{}, using default: {}",
                            owner, repository, policyEx.getMessage());
                }
            }
            java.util.Set<String> enabledRules = (effectivePolicy != null) ? effectivePolicy.getEnabledRuleIdsSet() : null;

            // Hybrid pipeline: evaluate deterministic rules and merge with AI findings
            ReviewResult reviewResult = aiReviewResult;
            if (deterministicRuleEngine != null && reviewInput != null) {
                try {
                    com.pushkar.codereview.github.review.rule.ReviewAnalysisContext analysisContext =
                            com.pushkar.codereview.github.review.rule.ReviewAnalysisContext.fromReviewInput(reviewInput, commitId);
                    List<com.pushkar.codereview.github.review.rule.RuleFinding> ruleFindings =
                            deterministicRuleEngine.evaluate(analysisContext, enabledRules);
                    reviewResult = findingMerger.merge(aiReviewResult, ruleFindings);
                } catch (Exception ruleEx) {
                    log.warn("Deterministic rule evaluation failed for reviewId={}, falling back to AI findings: {}",
                            reviewId, ruleEx.getMessage(), ruleEx);
                }
            }

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

            // Evaluate and persist quality gate for completed review
            if (qualityGateService != null && reviewId != null) {
                try {
                    qualityGateService.evaluateAndSave(reviewId, effectivePolicy);
                } catch (Exception qgEx) {
                    log.warn("Quality gate evaluation failed for reviewId={}: {}", reviewId, qgEx.getMessage(), qgEx);
                }
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
            if (codeReviewMetrics != null) {
                codeReviewMetrics.decrementInProgress();
            }
            MDC.remove("correlationId");
            MDC.remove("reviewId");
        }
    }
}
