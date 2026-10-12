package com.pushkar.codereview.observability;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.config.WebhookMetrics;
import com.pushkar.codereview.config.resilience.ResilienceExecutor;
import com.pushkar.codereview.observability.dto.ObservabilitySummaryDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    private final CodeReviewMetrics codeReviewMetrics;
    private final WebhookMetrics webhookMetrics;
    private final MeterRegistry meterRegistry;
    private final ResilienceExecutor resilienceExecutor;
    private final ApplicationAvailability applicationAvailability;
    private final DataSource dataSource;

    @Autowired
    public ObservabilityService(@Autowired(required = false) CodeReviewMetrics codeReviewMetrics,
                                @Autowired(required = false) WebhookMetrics webhookMetrics,
                                @Autowired(required = false) MeterRegistry meterRegistry,
                                @Autowired(required = false) ResilienceExecutor resilienceExecutor,
                                @Autowired(required = false) ApplicationAvailability applicationAvailability,
                                @Autowired(required = false) DataSource dataSource) {
        this.codeReviewMetrics = codeReviewMetrics;
        this.webhookMetrics = webhookMetrics;
        this.meterRegistry = meterRegistry;
        this.resilienceExecutor = resilienceExecutor;
        this.applicationAvailability = applicationAvailability;
        this.dataSource = dataSource;
    }

    public ObservabilitySummaryDto getObservabilitySummary() {
        ObservabilitySummaryDto.ApplicationHealthDto health = resolveApplicationHealth();
        ObservabilitySummaryDto.ReviewWorkflowMetricsDto reviewMetrics = resolveReviewMetrics();
        ObservabilitySummaryDto.WebhookReliabilityMetricsDto webhookMetricsDto = resolveWebhookMetrics();
        ObservabilitySummaryDto.ExternalServicesMetricsDto externalServices = resolveExternalServicesMetrics();
        ObservabilitySummaryDto.QualityGateMetricsDto qualityGate = resolveQualityGateMetrics();
        ObservabilitySummaryDto.SystemMetricsDto system = resolveSystemMetrics();

        return new ObservabilitySummaryDto(
                health,
                reviewMetrics,
                webhookMetricsDto,
                externalServices,
                qualityGate,
                system
        );
    }

    private ObservabilitySummaryDto.ApplicationHealthDto resolveApplicationHealth() {
        String liveness = "UP";
        if (applicationAvailability != null) {
            liveness = (applicationAvailability.getLivenessState() == LivenessState.CORRECT) ? "UP" : "DOWN";
        }

        String readiness = "UP";
        if (applicationAvailability != null) {
            readiness = (applicationAvailability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC) ? "UP" : "DOWN";
        }

        String database = "UNKNOWN";
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                database = (conn != null && conn.isValid(2)) ? "UP" : "DOWN";
            } catch (Exception ex) {
                log.debug("Database health check probe encountered error: {}", ex.getMessage());
                database = "DOWN";
            }
        }

        String overallStatus = ("UP".equals(liveness) && "UP".equals(readiness) && !"DOWN".equals(database)) ? "UP" : "DEGRADED";
        if ("DOWN".equals(liveness) || "DOWN".equals(readiness)) {
            overallStatus = "DOWN";
        }

        return new ObservabilitySummaryDto.ApplicationHealthDto(overallStatus, liveness, readiness, database);
    }

    private ObservabilitySummaryDto.ReviewWorkflowMetricsDto resolveReviewMetrics() {
        if (codeReviewMetrics == null) {
            return new ObservabilitySummaryDto.ReviewWorkflowMetricsDto(0, 0, 0, 0, 0, 0,
                    new ObservabilitySummaryDto.FindingsBySourceDto(0, 0, 0));
        }

        double submissionsTotal = getCounterCount(codeReviewMetrics.getSubmissionsCounter());
        double duplicateSubmissionsTotal = getCounterCount(codeReviewMetrics.getDuplicateSubmissionsCounter());
        double completedTotal = getCounterCount(codeReviewMetrics.getCompletedCounter());
        double failedTotal = getCounterCount(codeReviewMetrics.getFailedCounter());
        int inProgress = codeReviewMetrics.getInProgressCount();

        Timer durationTimer = codeReviewMetrics.getDurationTimer();
        double avgDurationMs = 0.0;
        if (durationTimer != null && durationTimer.count() > 0) {
            avgDurationMs = durationTimer.totalTime(TimeUnit.MILLISECONDS) / durationTimer.count();
        }

        double aiFindings = getCounterCount(codeReviewMetrics.getFindingsBySourceCounter("ai"));
        double ruleFindings = getCounterCount(codeReviewMetrics.getFindingsBySourceCounter("deterministic_rules"));
        double totalFindings = getCounterCount(codeReviewMetrics.getFindingsCounter());
        if (totalFindings == 0 && (aiFindings > 0 || ruleFindings > 0)) {
            totalFindings = aiFindings + ruleFindings;
        }

        ObservabilitySummaryDto.FindingsBySourceDto findings = new ObservabilitySummaryDto.FindingsBySourceDto(
                aiFindings, ruleFindings, totalFindings
        );

        return new ObservabilitySummaryDto.ReviewWorkflowMetricsDto(
                submissionsTotal,
                duplicateSubmissionsTotal,
                completedTotal,
                failedTotal,
                inProgress,
                Math.round(avgDurationMs * 10.0) / 10.0,
                findings
        );
    }

    private ObservabilitySummaryDto.WebhookReliabilityMetricsDto resolveWebhookMetrics() {
        if (webhookMetrics == null) {
            return new ObservabilitySummaryDto.WebhookReliabilityMetricsDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double received = getCounterCount(webhookMetrics.getDeliveriesReceivedCounter());
        double validSig = getCounterCount(webhookMetrics.getSignatureCounter(true));
        double invalidSig = getCounterCount(webhookMetrics.getSignatureCounter(false));

        double accepted = getCounterCount(webhookMetrics.getDeliveryStatusCounter("ACCEPTED"));
        double ignored = getCounterCount(webhookMetrics.getDeliveryStatusCounter("IGNORED"));
        double completed = getCounterCount(webhookMetrics.getDeliveryStatusCounter("COMPLETED"));
        double failed = getCounterCount(webhookMetrics.getDeliveryStatusCounter("FAILED"));
        double duplicates = getCounterCount(webhookMetrics.getDuplicateDeliveriesCounter());

        double retriesAttempts = getCounterCount(webhookMetrics.getRetryAttemptsCounter());
        double retriesExhausted = getCounterCount(webhookMetrics.getRetriesExhaustedCounter());
        double staleRecovered = getCounterCount(webhookMetrics.getStaleRecoveredCounter());
        double retriesProcessed = getCounterCount(webhookMetrics.getRetriesProcessedCounter());

        Timer timer = webhookMetrics.getWebhookProcessingTimer();
        double avgDurationMs = 0.0;
        if (timer != null && timer.count() > 0) {
            avgDurationMs = timer.totalTime(TimeUnit.MILLISECONDS) / timer.count();
        }

        return new ObservabilitySummaryDto.WebhookReliabilityMetricsDto(
                received,
                validSig,
                invalidSig,
                accepted,
                ignored,
                completed,
                failed,
                duplicates,
                retriesAttempts,
                retriesExhausted,
                Math.round(avgDurationMs * 10.0) / 10.0,
                staleRecovered,
                retriesProcessed
        );
    }

    private ObservabilitySummaryDto.ExternalServicesMetricsDto resolveExternalServicesMetrics() {
        String geminiCbState = "CLOSED";
        String githubCbState = "CLOSED";

        if (resilienceExecutor != null) {
            CircuitBreaker geminiCb = resilienceExecutor.getCircuitBreaker("gemini");
            if (geminiCb != null) geminiCbState = geminiCb.getState().name();

            CircuitBreaker githubCb = resilienceExecutor.getCircuitBreaker("github");
            if (githubCb != null) githubCbState = githubCb.getState().name();
        }

        double geminiRetries = (codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getRetryCounter("gemini")) : 0.0;
        double githubRetries = (codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getRetryCounter("github")) : 0.0;

        double geminiRateLimits = (codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getRateLimitCounter("gemini")) : 0.0;
        double githubRateLimits = (codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getRateLimitCounter("github")) : 0.0;

        double geminiSuccess = 0.0;
        double geminiFailed = 0.0;
        double geminiAvgDuration = 0.0;
        if (codeReviewMetrics != null) {
            geminiSuccess = getCounterCount(codeReviewMetrics.getApiRequestSuccessCounter("gemini"));
            geminiFailed = getCounterCount(codeReviewMetrics.getApiRequestFailedCounter("gemini"));
            Timer geminiTimer = codeReviewMetrics.getApiDurationTimer("gemini");
            if (geminiTimer == null) {
                geminiTimer = codeReviewMetrics.getAiExecutionTimer();
            }
            if (geminiTimer != null && geminiTimer.count() > 0) {
                geminiAvgDuration = geminiTimer.totalTime(TimeUnit.MILLISECONDS) / geminiTimer.count();
                if (geminiSuccess == 0) {
                    geminiSuccess = geminiTimer.count();
                }
            }
        }
        double geminiFailures = geminiFailed > 0 ? geminiFailed : ((codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getExternalFailureCounter("gemini")) : 0.0);

        double githubSuccess = 0.0;
        double githubFailed = 0.0;
        double githubAvgDuration = 0.0;
        if (codeReviewMetrics != null) {
            githubSuccess = getCounterCount(codeReviewMetrics.getApiRequestSuccessCounter("github"));
            githubFailed = getCounterCount(codeReviewMetrics.getApiRequestFailedCounter("github"));
            Timer ghTimer = codeReviewMetrics.getApiDurationTimer("github");
            if (ghTimer != null && ghTimer.count() > 0) {
                githubAvgDuration = ghTimer.totalTime(TimeUnit.MILLISECONDS) / ghTimer.count();
            }
        }
        if (githubSuccess == 0 && meterRegistry != null) {
            try {
                Counter ghSuccessCounter = meterRegistry.find("github.api.request").tag("status", "success").counter();
                if (ghSuccessCounter != null) githubSuccess = ghSuccessCounter.count();

                Timer ghTimer = meterRegistry.find("github.api.duration").timer();
                if (ghTimer != null && ghTimer.count() > 0 && githubAvgDuration == 0) {
                    githubAvgDuration = ghTimer.totalTime(TimeUnit.MILLISECONDS) / ghTimer.count();
                }
            } catch (Exception ignored) {}
        }
        double githubFailures = githubFailed > 0 ? githubFailed : ((codeReviewMetrics != null) ? getCounterCount(codeReviewMetrics.getGithubFailuresCounter()) : 0.0);

        ObservabilitySummaryDto.DependencyMetricDto gemini = new ObservabilitySummaryDto.DependencyMetricDto(
                geminiSuccess,
                geminiFailures,
                geminiRetries,
                geminiCbState,
                geminiRateLimits,
                Math.round(geminiAvgDuration * 10.0) / 10.0
        );

        ObservabilitySummaryDto.DependencyMetricDto github = new ObservabilitySummaryDto.DependencyMetricDto(
                githubSuccess,
                githubFailures,
                githubRetries,
                githubCbState,
                githubRateLimits,
                Math.round(githubAvgDuration * 10.0) / 10.0
        );

        return new ObservabilitySummaryDto.ExternalServicesMetricsDto(gemini, github);
    }

    private ObservabilitySummaryDto.QualityGateMetricsDto resolveQualityGateMetrics() {
        if (codeReviewMetrics == null) {
            return new ObservabilitySummaryDto.QualityGateMetricsDto(0, 0, 0);
        }

        double passed = getCounterCount(codeReviewMetrics.getQualityGateCounter("PASS"));
        double failed = getCounterCount(codeReviewMetrics.getQualityGateCounter("FAIL"));
        double notEvaluated = getCounterCount(codeReviewMetrics.getQualityGateCounter("NOT_EVALUATED"));

        return new ObservabilitySummaryDto.QualityGateMetricsDto(passed, failed, notEvaluated);
    }

    private ObservabilitySummaryDto.SystemMetricsDto resolveSystemMetrics() {
        double uptimeSec = 0.0;
        try {
            uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
        } catch (Exception ignored) {}

        return new ObservabilitySummaryDto.SystemMetricsDto(Math.round(uptimeSec), Instant.now());
    }

    private double getCounterCount(Counter counter) {
        return (counter != null) ? counter.count() : 0.0;
    }
}
