package com.pushkar.codereview.observability;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.config.WebhookMetrics;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.AsyncCodeReviewRunner;
import com.pushkar.codereview.github.review.GithubPullRequestReviewService;
import com.pushkar.codereview.github.review.GithubReviewCommentService;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.github.review.rule.DeterministicRuleEngine;
import com.pushkar.codereview.github.review.rule.ReviewFindingMerger;
import com.pushkar.codereview.github.review.rule.RuleFinding;
import com.pushkar.codereview.policy.RepositoryReviewPolicy;
import com.pushkar.codereview.qualitygate.CodeReviewQualityGate;
import com.pushkar.codereview.qualitygate.QualityGateService;
import com.pushkar.codereview.qualitygate.QualityGateStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityWorkflowMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private CodeReviewMetrics codeReviewMetrics;
    private WebhookMetrics webhookMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        codeReviewMetrics = new CodeReviewMetrics(meterRegistry);
        webhookMetrics = new WebhookMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Review success metrics increment correctly and duration is recorded")
    void testReviewSuccessMetrics_IncrementCorrectly() {
        StubPullRequestReviewService prService = new StubPullRequestReviewService();
        StubAiReviewService aiService = new StubAiReviewService();
        StubReviewCommentService commentService = new StubReviewCommentService();
        StubPersistenceService persistenceService = new StubPersistenceService();
        StubRuleEngine ruleEngine = new StubRuleEngine();
        StubQualityGateService qualityGateService = new StubQualityGateService();

        ReviewInput sampleInput = new ReviewInput(
                100L, "hello-world", "octocat/hello-world", "https://github.com/octocat/hello-world", "main",
                200L, 42L, "PR Title", "PR Body", "open",
                "https://github.com/octocat/hello-world/pull/42", "octocat", "sha999", "main",
                Instant.now(), Instant.now(), List.of(new ReviewFileInput("Main.java", "modified", 5, 1, 6, "@@ -1 +1 @@", null))
        );
        ReviewFinding aiFinding = new ReviewFinding("Main.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "AI Bug", "Fix");
        ReviewResult aiResult = new ReviewResult("AI Summary", List.of(aiFinding));

        RuleFinding ruleFinding = new RuleFinding("SEC-001", "Main.java", 12, 12,
                ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY, "Hardcoded secret", "Remove secret");

        prService.setReviewInput(sampleInput);
        aiService.setReviewResult(aiResult);
        ruleEngine.setRuleFindings(List.of(ruleFinding));
        commentService.setCommentResponses(List.of(
                new GithubReviewCommentResponse(1L, "comment", "Main.java", 10, "sha999", "url", Instant.now())
        ));
        qualityGateService.setGateStatus(QualityGateStatus.PASS);

        AsyncCodeReviewRunner runner = new AsyncCodeReviewRunner(
                prService, aiService, commentService, persistenceService, codeReviewMetrics,
                ruleEngine, new ReviewFindingMerger(), qualityGateService, null
        );

        // Execute review
        runner.executeReviewAsync(10L, 1L, "octocat", "hello-world", 42L, "corr-123");

        // Verify completed metrics
        assertThat(codeReviewMetrics.getCompletedCounter().count()).isEqualTo(1.0);
        assertThat(codeReviewMetrics.getFailedCounter().count()).isEqualTo(0.0);
        assertThat(codeReviewMetrics.getOutcomeCounter("COMPLETED").count()).isEqualTo(1.0);
        assertThat(codeReviewMetrics.getExecutionTimer().count()).isEqualTo(1L);

        // Verify findings by source
        assertThat(codeReviewMetrics.getFindingsBySourceCounter("ai").count()).isEqualTo(1.0);
        assertThat(codeReviewMetrics.getFindingsBySourceCounter("deterministic_rules").count()).isEqualTo(1.0);

        // Verify quality gate outcome
        assertThat(codeReviewMetrics.getQualityGateCounter("PASS").count()).isEqualTo(1.0);

        // Verify in-progress gauge was decremented back to 0
        assertThat(codeReviewMetrics.getInProgressCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Review failure metrics increment correctly and duration is recorded")
    void testReviewFailureMetrics_IncrementCorrectly() {
        StubPullRequestReviewService prService = new StubPullRequestReviewService();
        prService.setThrowError(true);
        StubAiReviewService aiService = new StubAiReviewService();
        StubReviewCommentService commentService = new StubReviewCommentService();
        StubPersistenceService persistenceService = new StubPersistenceService();

        AsyncCodeReviewRunner runner = new AsyncCodeReviewRunner(
                prService, aiService, commentService, persistenceService, codeReviewMetrics,
                null, null, null, null
        );

        runner.executeReviewAsync(20L, 1L, "octocat", "hello-world", 42L, "corr-fail-456");

        assertThat(codeReviewMetrics.getFailedCounter().count()).isEqualTo(1.0);
        assertThat(codeReviewMetrics.getCompletedCounter().count()).isEqualTo(0.0);
        assertThat(codeReviewMetrics.getOutcomeCounter("FAILED").count()).isEqualTo(1.0);
        assertThat(codeReviewMetrics.getExecutionTimer().count()).isEqualTo(1L);
        assertThat(codeReviewMetrics.getInProgressCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Webhook metrics record accepted, ignored, duplicate, signatures, and retries accurately")
    void testWebhookMetrics_RecordCorrectly() {
        webhookMetrics.recordDeliveryReceived();
        webhookMetrics.recordDeliveryReceived();
        assertThat(webhookMetrics.getDeliveriesReceivedCounter().count()).isEqualTo(2.0);

        webhookMetrics.recordSignatureVerification(true);
        webhookMetrics.recordSignatureVerification(false);
        assertThat(webhookMetrics.getSignatureCounter(true).count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getSignatureCounter(false).count()).isEqualTo(1.0);

        webhookMetrics.recordDeliveryStatus("ACCEPTED");
        webhookMetrics.recordDeliveryStatus("IGNORED");
        webhookMetrics.recordDeliveryStatus("COMPLETED");
        webhookMetrics.recordDeliveryStatus("FAILED");

        assertThat(webhookMetrics.getDeliveryStatusCounter("ACCEPTED").count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getDeliveryStatusCounter("IGNORED").count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getDeliveryStatusCounter("COMPLETED").count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getDeliveryStatusCounter("FAILED").count()).isEqualTo(1.0);

        webhookMetrics.recordDuplicateDelivery();
        assertThat(webhookMetrics.getDuplicateDeliveriesCounter().count()).isEqualTo(1.0);

        webhookMetrics.recordRetryAttempt();
        webhookMetrics.recordRetryExhausted();
        assertThat(webhookMetrics.getRetryAttemptsCounter().count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getRetriesExhaustedCounter().count()).isEqualTo(1.0);

        webhookMetrics.recordStaleRecovered(3);
        webhookMetrics.recordRetriesProcessed(2);
        assertThat(webhookMetrics.getStaleRecoveredCounter().count()).isEqualTo(3.0);
        assertThat(webhookMetrics.getRetriesProcessedCounter().count()).isEqualTo(2.0);

        webhookMetrics.recordProcessingTime(150);
        assertThat(webhookMetrics.getWebhookProcessingTimer().count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("MDC correlation ID and review ID propagate into async review and clean up after execution")
    void testCorrelationId_PropagationAndCleanup() {
        StubPullRequestReviewService prService = new StubPullRequestReviewService();
        StubAiReviewService aiService = new StubAiReviewService();
        StubReviewCommentService commentService = new StubReviewCommentService();
        StubPersistenceService persistenceService = new StubPersistenceService();

        String testCorrelationId = "safe-corr-id-777";
        Long reviewId = 99L;

        final String[] capturedMdc = new String[2];

        prService.setOnExecuteCallback(() -> {
            capturedMdc[0] = MDC.get("correlationId");
            capturedMdc[1] = MDC.get("reviewId");
        });
        prService.setReviewInput(new ReviewInput(1L, "repo", "owner/repo", "url", "main", 1L, 1L, "title", "body", "open", "url", "owner", "sha", "main", Instant.now(), Instant.now(), List.of()));
        aiService.setReviewResult(new ReviewResult("summary", List.of()));

        AsyncCodeReviewRunner runner = new AsyncCodeReviewRunner(
                prService, aiService, commentService, persistenceService, codeReviewMetrics
        );

        runner.executeReviewAsync(reviewId, 1L, "owner", "repo", 1L, testCorrelationId);

        // In execution:
        assertThat(capturedMdc[0]).isEqualTo(testCorrelationId);
        assertThat(capturedMdc[1]).isEqualTo(String.valueOf(reviewId));

        // After execution: context cleaned up
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("reviewId")).isNull();
    }

    @Test
    @DisplayName("Metric tags do not contain unbound, sensitive, or high-cardinality values")
    void testMetricTags_AreStrictlyBounded() {
        codeReviewMetrics.recordSubmission(false);
        codeReviewMetrics.recordSubmission(true);
        codeReviewMetrics.recordCompleted();
        codeReviewMetrics.recordFailed();
        codeReviewMetrics.recordOutcome("COMPLETED");
        codeReviewMetrics.recordOutcome("FAILED");
        codeReviewMetrics.recordFindingsBySource("ai", 5);
        codeReviewMetrics.recordFindingsBySource("deterministic_rules", 3);
        codeReviewMetrics.recordQualityGateOutcome("PASS");
        codeReviewMetrics.recordQualityGateOutcome("FAIL");
        codeReviewMetrics.recordRateLimit("github");

        webhookMetrics.recordDeliveryReceived();
        webhookMetrics.recordSignatureVerification(true);
        webhookMetrics.recordSignatureVerification(false);
        webhookMetrics.recordDeliveryStatus("ACCEPTED");
        webhookMetrics.recordDeliveryStatus("IGNORED");
        webhookMetrics.recordDuplicateDelivery();

        Set<String> forbiddenTagKeys = Set.of(
                "repo", "repository", "owner", "user", "userid", "user_id",
                "pr", "pullrequest", "pull_request", "sha", "commit", "commit_sha",
                "delivery", "delivery_id", "deliveryid", "correlation_id", "correlationid",
                "secret", "token", "payload", "signature"
        );

        for (Meter meter : meterRegistry.getMeters()) {
            meter.getId().getTags().forEach(tag -> {
                String key = tag.getKey().toLowerCase();
                String value = tag.getValue().toLowerCase();

                assertThat(forbiddenTagKeys)
                        .withFailMessage("Meter %s contains forbidden tag key: %s", meter.getId().getName(), key)
                        .doesNotContain(key);

                assertThat(value)
                        .withFailMessage("Tag value in meter %s looks like a SHA or UUID: %s", meter.getId().getName(), value)
                        .doesNotMatch("^[0-9a-f]{40}$")
                        .doesNotMatch("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
            });
        }
    }

    @Test
    @DisplayName("ObservabilityService correctly builds bounded summary DTO from metrics")
    void testObservabilityService_BuildsCorrectSummaryDto() {
        codeReviewMetrics.recordSubmission(false);
        codeReviewMetrics.recordCompleted();
        codeReviewMetrics.recordFindingsBySource("ai", 4);
        codeReviewMetrics.recordFindingsBySource("deterministic_rules", 2);
        codeReviewMetrics.recordQualityGateOutcome("PASS");

        webhookMetrics.recordDeliveryReceived();
        webhookMetrics.recordSignatureVerification(true);
        webhookMetrics.recordDeliveryStatus("ACCEPTED");

        ObservabilityService service = new ObservabilityService(
                codeReviewMetrics, webhookMetrics, meterRegistry, null, null, null
        );

        var summary = service.getObservabilitySummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getApplicationHealth()).isNotNull();
        assertThat(summary.getReviewMetrics()).isNotNull();
        assertThat(summary.getReviewMetrics().getCompletedTotal()).isEqualTo(1.0);
        assertThat(summary.getReviewMetrics().getFindings().getAi()).isEqualTo(4.0);
        assertThat(summary.getReviewMetrics().getFindings().getDeterministicRules()).isEqualTo(2.0);

        assertThat(summary.getWebhookMetrics()).isNotNull();
        assertThat(summary.getWebhookMetrics().getDeliveriesReceivedTotal()).isEqualTo(1.0);
        assertThat(summary.getWebhookMetrics().getSignaturesValidTotal()).isEqualTo(1.0);
        assertThat(summary.getWebhookMetrics().getAcceptedTotal()).isEqualTo(1.0);

        assertThat(summary.getQualityGateMetrics().getPassedTotal()).isEqualTo(1.0);
        assertThat(summary.getSystem().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("External dependency requests and latencies are tracked accurately in summary")
    void testExternalDependencyMetrics_TrackedAccurately() {
        codeReviewMetrics.recordExternalApiRequest("github", true, 120);
        codeReviewMetrics.recordExternalApiRequest("gemini", true, 850);
        codeReviewMetrics.recordRateLimit("github");

        ObservabilityService service = new ObservabilityService(
                codeReviewMetrics, webhookMetrics, meterRegistry, null, null, null
        );

        var summary = service.getObservabilitySummary();
        var external = summary.getExternalServices();

        assertThat(external).isNotNull();
        assertThat(external.getGithub().getRequestsSuccess()).isEqualTo(1.0);
        assertThat(external.getGithub().getAverageDurationMs()).isEqualTo(120.0);
        assertThat(external.getGithub().getRateLimitsTotal()).isEqualTo(1.0);

        assertThat(external.getGemini().getRequestsSuccess()).isEqualTo(1.0);
        assertThat(external.getGemini().getAverageDurationMs()).isEqualTo(850.0);
    }

    @Test
    @DisplayName("Webhook duplicate delivery does not increment delivery status outcome counters")
    void testWebhookDuplicateDelivery_DoesNotIncrementStatusOutcomes() {
        // Normal delivery completed
        webhookMetrics.recordDeliveryReceived();
        webhookMetrics.recordSignatureVerification(true);
        webhookMetrics.recordDeliveryStatus("COMPLETED");

        // Duplicate delivery arriving
        webhookMetrics.recordDeliveryReceived();
        webhookMetrics.recordDuplicateDelivery();

        assertThat(webhookMetrics.getDeliveriesReceivedCounter().count()).isEqualTo(2.0);
        assertThat(webhookMetrics.getDuplicateDeliveriesCounter().count()).isEqualTo(1.0);
        assertThat(webhookMetrics.getDeliveryStatusCounter("COMPLETED").count()).isEqualTo(1.0);
        // Ensure no spurious IGNORED status recorded
        assertThat(webhookMetrics.getDeliveryStatusCounter("IGNORED")).isNull();
    }

    // --- Concrete Test Stubs to Avoid Bytecode Mocking on JDK 26 ---

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        private ReviewInput reviewInput;
        private Runnable onExecuteCallback;
        private boolean throwError;

        public StubPullRequestReviewService() { super(null, null, null, null); }

        public void setReviewInput(ReviewInput reviewInput) { this.reviewInput = reviewInput; }
        public void setOnExecuteCallback(Runnable callback) { this.onExecuteCallback = callback; }
        public void setThrowError(boolean throwError) { this.throwError = throwError; }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            if (throwError) {
                throw new RuntimeException("Simulated error in PR review input fetch");
            }
            if (onExecuteCallback != null) {
                onExecuteCallback.run();
            }
            return reviewInput != null ? reviewInput : new ReviewInput(1L, "repo", "owner/repo", "url", "main", 1L, 1L, "title", "body", "open", "url", "owner", "sha", "main", Instant.now(), Instant.now(), List.of());
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
        private List<GithubReviewCommentResponse> commentResponses = List.of();

        public StubReviewCommentService() { super(null); }

        public void setCommentResponses(List<GithubReviewCommentResponse> commentResponses) { this.commentResponses = commentResponses; }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            return commentResponses;
        }
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        public StubPersistenceService() { super(null); }

        @Override
        public List<CodeReviewFinding> saveFindings(Long reviewId, List<ReviewFinding> findings) { return List.of(); }

        @Override
        public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
            CodeReview cr = new CodeReview(12345L, "octocat", "hello-world", 42);
            cr.setId(reviewId);
            cr.setStatus(CodeReviewStatus.COMPLETED);
            return cr;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            CodeReview cr = new CodeReview(12345L, "octocat", "hello-world", 42);
            cr.setId(reviewId);
            cr.setStatus(CodeReviewStatus.FAILED);
            return cr;
        }
    }

    private static class StubRuleEngine extends DeterministicRuleEngine {
        private List<RuleFinding> ruleFindings = List.of();

        public StubRuleEngine() { super(null); }

        public void setRuleFindings(List<RuleFinding> findings) { this.ruleFindings = findings; }

        @Override
        public List<RuleFinding> evaluate(com.pushkar.codereview.github.review.rule.ReviewAnalysisContext context, Set<String> enabledRuleIds) {
            return ruleFindings;
        }
    }

    private static class StubQualityGateService extends QualityGateService {
        private QualityGateStatus status = QualityGateStatus.PASS;

        public StubQualityGateService() { super(null, null, null, null, null, null); }

        public void setGateStatus(QualityGateStatus status) { this.status = status; }

        @Override
        public Optional<CodeReviewQualityGate> evaluateAndSave(Long reviewId, RepositoryReviewPolicy policy) {
            CodeReview cr = new CodeReview(1L, "octocat", "hello-world", 42);
            cr.setId(reviewId);
            CodeReviewQualityGate gate = new CodeReviewQualityGate(cr, status, true, null, 0, "Evaluated", Instant.now());
            return Optional.of(gate);
        }
    }
}
