package com.pushkar.codereview.observability.dto;

import java.time.Instant;

public class ObservabilitySummaryDto {

    private ApplicationHealthDto applicationHealth;
    private ReviewWorkflowMetricsDto reviewMetrics;
    private WebhookReliabilityMetricsDto webhookMetrics;
    private ExternalServicesMetricsDto externalServices;
    private QualityGateMetricsDto qualityGateMetrics;
    private SystemMetricsDto system;

    public ObservabilitySummaryDto() {
    }

    public ObservabilitySummaryDto(ApplicationHealthDto applicationHealth,
                                  ReviewWorkflowMetricsDto reviewMetrics,
                                  WebhookReliabilityMetricsDto webhookMetrics,
                                  ExternalServicesMetricsDto externalServices,
                                  QualityGateMetricsDto qualityGateMetrics,
                                  SystemMetricsDto system) {
        this.applicationHealth = applicationHealth;
        this.reviewMetrics = reviewMetrics;
        this.webhookMetrics = webhookMetrics;
        this.externalServices = externalServices;
        this.qualityGateMetrics = qualityGateMetrics;
        this.system = system;
    }

    public ApplicationHealthDto getApplicationHealth() {
        return applicationHealth;
    }

    public void setApplicationHealth(ApplicationHealthDto applicationHealth) {
        this.applicationHealth = applicationHealth;
    }

    public ReviewWorkflowMetricsDto getReviewMetrics() {
        return reviewMetrics;
    }

    public void setReviewMetrics(ReviewWorkflowMetricsDto reviewMetrics) {
        this.reviewMetrics = reviewMetrics;
    }

    public WebhookReliabilityMetricsDto getWebhookMetrics() {
        return webhookMetrics;
    }

    public void setWebhookMetrics(WebhookReliabilityMetricsDto webhookMetrics) {
        this.webhookMetrics = webhookMetrics;
    }

    public ExternalServicesMetricsDto getExternalServices() {
        return externalServices;
    }

    public void setExternalServices(ExternalServicesMetricsDto externalServices) {
        this.externalServices = externalServices;
    }

    public QualityGateMetricsDto getQualityGateMetrics() {
        return qualityGateMetrics;
    }

    public void setQualityGateMetrics(QualityGateMetricsDto qualityGateMetrics) {
        this.qualityGateMetrics = qualityGateMetrics;
    }

    public SystemMetricsDto getSystem() {
        return system;
    }

    public void setSystem(SystemMetricsDto system) {
        this.system = system;
    }

    public static class ApplicationHealthDto {
        private String status;
        private String liveness;
        private String readiness;
        private String database;

        public ApplicationHealthDto() {}

        public ApplicationHealthDto(String status, String liveness, String readiness, String database) {
            this.status = status;
            this.liveness = liveness;
            this.readiness = readiness;
            this.database = database;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLiveness() { return liveness; }
        public void setLiveness(String liveness) { this.liveness = liveness; }
        public String getReadiness() { return readiness; }
        public void setReadiness(String readiness) { this.readiness = readiness; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
    }

    public static class ReviewWorkflowMetricsDto {
        private double submissionsTotal;
        private double duplicateSubmissionsTotal;
        private double completedTotal;
        private double failedTotal;
        private int inProgressCount;
        private double averageDurationMs;
        private FindingsBySourceDto findings;

        public ReviewWorkflowMetricsDto() {}

        public ReviewWorkflowMetricsDto(double submissionsTotal, double duplicateSubmissionsTotal,
                                        double completedTotal, double failedTotal,
                                        int inProgressCount, double averageDurationMs,
                                        FindingsBySourceDto findings) {
            this.submissionsTotal = submissionsTotal;
            this.duplicateSubmissionsTotal = duplicateSubmissionsTotal;
            this.completedTotal = completedTotal;
            this.failedTotal = failedTotal;
            this.inProgressCount = inProgressCount;
            this.averageDurationMs = averageDurationMs;
            this.findings = findings;
        }

        public double getSubmissionsTotal() { return submissionsTotal; }
        public void setSubmissionsTotal(double submissionsTotal) { this.submissionsTotal = submissionsTotal; }
        public double getDuplicateSubmissionsTotal() { return duplicateSubmissionsTotal; }
        public void setDuplicateSubmissionsTotal(double duplicateSubmissionsTotal) { this.duplicateSubmissionsTotal = duplicateSubmissionsTotal; }
        public double getCompletedTotal() { return completedTotal; }
        public void setCompletedTotal(double completedTotal) { this.completedTotal = completedTotal; }
        public double getFailedTotal() { return failedTotal; }
        public void setFailedTotal(double failedTotal) { this.failedTotal = failedTotal; }
        public int getInProgressCount() { return inProgressCount; }
        public void setInProgressCount(int inProgressCount) { this.inProgressCount = inProgressCount; }
        public double getAverageDurationMs() { return averageDurationMs; }
        public void setAverageDurationMs(double averageDurationMs) { this.averageDurationMs = averageDurationMs; }
        public FindingsBySourceDto getFindings() { return findings; }
        public void setFindings(FindingsBySourceDto findings) { this.findings = findings; }
    }

    public static class FindingsBySourceDto {
        private double ai;
        private double deterministicRules;
        private double total;

        public FindingsBySourceDto() {}

        public FindingsBySourceDto(double ai, double deterministicRules, double total) {
            this.ai = ai;
            this.deterministicRules = deterministicRules;
            this.total = total;
        }

        public double getAi() { return ai; }
        public void setAi(double ai) { this.ai = ai; }
        public double getDeterministicRules() { return deterministicRules; }
        public void setDeterministicRules(double deterministicRules) { this.deterministicRules = deterministicRules; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }

    public static class WebhookReliabilityMetricsDto {
        private double deliveriesReceivedTotal;
        private double signaturesValidTotal;
        private double signaturesInvalidTotal;
        private double acceptedTotal;
        private double ignoredTotal;
        private double completedTotal;
        private double failedTotal;
        private double duplicateTotal;
        private double retryAttemptsTotal;
        private double retriesExhaustedTotal;
        private double averageDurationMs;
        private double staleRecoveredTotal;
        private double pendingRetriesProcessedTotal;

        public WebhookReliabilityMetricsDto() {}

        public WebhookReliabilityMetricsDto(double deliveriesReceivedTotal, double signaturesValidTotal,
                                           double signaturesInvalidTotal, double acceptedTotal,
                                           double ignoredTotal, double completedTotal,
                                           double failedTotal, double duplicateTotal,
                                           double retryAttemptsTotal, double retriesExhaustedTotal,
                                           double averageDurationMs, double staleRecoveredTotal,
                                           double pendingRetriesProcessedTotal) {
            this.deliveriesReceivedTotal = deliveriesReceivedTotal;
            this.signaturesValidTotal = signaturesValidTotal;
            this.signaturesInvalidTotal = signaturesInvalidTotal;
            this.acceptedTotal = acceptedTotal;
            this.ignoredTotal = ignoredTotal;
            this.completedTotal = completedTotal;
            this.failedTotal = failedTotal;
            this.duplicateTotal = duplicateTotal;
            this.retryAttemptsTotal = retryAttemptsTotal;
            this.retriesExhaustedTotal = retriesExhaustedTotal;
            this.averageDurationMs = averageDurationMs;
            this.staleRecoveredTotal = staleRecoveredTotal;
            this.pendingRetriesProcessedTotal = pendingRetriesProcessedTotal;
        }

        public double getDeliveriesReceivedTotal() { return deliveriesReceivedTotal; }
        public void setDeliveriesReceivedTotal(double deliveriesReceivedTotal) { this.deliveriesReceivedTotal = deliveriesReceivedTotal; }
        public double getSignaturesValidTotal() { return signaturesValidTotal; }
        public void setSignaturesValidTotal(double signaturesValidTotal) { this.signaturesValidTotal = signaturesValidTotal; }
        public double getSignaturesInvalidTotal() { return signaturesInvalidTotal; }
        public void setSignaturesInvalidTotal(double signaturesInvalidTotal) { this.signaturesInvalidTotal = signaturesInvalidTotal; }
        public double getAcceptedTotal() { return acceptedTotal; }
        public void setAcceptedTotal(double acceptedTotal) { this.acceptedTotal = acceptedTotal; }
        public double getIgnoredTotal() { return ignoredTotal; }
        public void setIgnoredTotal(double ignoredTotal) { this.ignoredTotal = ignoredTotal; }
        public double getCompletedTotal() { return completedTotal; }
        public void setCompletedTotal(double completedTotal) { this.completedTotal = completedTotal; }
        public double getFailedTotal() { return failedTotal; }
        public void setFailedTotal(double failedTotal) { this.failedTotal = failedTotal; }
        public double getDuplicateTotal() { return duplicateTotal; }
        public void setDuplicateTotal(double duplicateTotal) { this.duplicateTotal = duplicateTotal; }
        public double getRetryAttemptsTotal() { return retryAttemptsTotal; }
        public void setRetryAttemptsTotal(double retryAttemptsTotal) { this.retryAttemptsTotal = retryAttemptsTotal; }
        public double getRetriesExhaustedTotal() { return retriesExhaustedTotal; }
        public void setRetriesExhaustedTotal(double retriesExhaustedTotal) { this.retriesExhaustedTotal = retriesExhaustedTotal; }
        public double getAverageDurationMs() { return averageDurationMs; }
        public void setAverageDurationMs(double averageDurationMs) { this.averageDurationMs = averageDurationMs; }
        public double getStaleRecoveredTotal() { return staleRecoveredTotal; }
        public void setStaleRecoveredTotal(double staleRecoveredTotal) { this.staleRecoveredTotal = staleRecoveredTotal; }
        public double getPendingRetriesProcessedTotal() { return pendingRetriesProcessedTotal; }
        public void setPendingRetriesProcessedTotal(double pendingRetriesProcessedTotal) { this.pendingRetriesProcessedTotal = pendingRetriesProcessedTotal; }
    }

    public static class ExternalServicesMetricsDto {
        private DependencyMetricDto gemini;
        private DependencyMetricDto github;

        public ExternalServicesMetricsDto() {}

        public ExternalServicesMetricsDto(DependencyMetricDto gemini, DependencyMetricDto github) {
            this.gemini = gemini;
            this.github = github;
        }

        public DependencyMetricDto getGemini() { return gemini; }
        public void setGemini(DependencyMetricDto gemini) { this.gemini = gemini; }
        public DependencyMetricDto getGithub() { return github; }
        public void setGithub(DependencyMetricDto github) { this.github = github; }
    }

    public static class DependencyMetricDto {
        private double requestsSuccess;
        private double requestsFailed;
        private double retriesTotal;
        private String circuitBreakerState;
        private double rateLimitsTotal;
        private double averageDurationMs;

        public DependencyMetricDto() {}

        public DependencyMetricDto(double requestsSuccess, double requestsFailed,
                                   double retriesTotal, String circuitBreakerState,
                                   double rateLimitsTotal, double averageDurationMs) {
            this.requestsSuccess = requestsSuccess;
            this.requestsFailed = requestsFailed;
            this.retriesTotal = retriesTotal;
            this.circuitBreakerState = circuitBreakerState;
            this.rateLimitsTotal = rateLimitsTotal;
            this.averageDurationMs = averageDurationMs;
        }

        public double getRequestsSuccess() { return requestsSuccess; }
        public void setRequestsSuccess(double requestsSuccess) { this.requestsSuccess = requestsSuccess; }
        public double getRequestsFailed() { return requestsFailed; }
        public void setRequestsFailed(double requestsFailed) { this.requestsFailed = requestsFailed; }
        public double getRetriesTotal() { return retriesTotal; }
        public void setRetriesTotal(double retriesTotal) { this.retriesTotal = retriesTotal; }
        public String getCircuitBreakerState() { return circuitBreakerState; }
        public void setCircuitBreakerState(String circuitBreakerState) { this.circuitBreakerState = circuitBreakerState; }
        public double getRateLimitsTotal() { return rateLimitsTotal; }
        public void setRateLimitsTotal(double rateLimitsTotal) { this.rateLimitsTotal = rateLimitsTotal; }
        public double getAverageDurationMs() { return averageDurationMs; }
        public void setAverageDurationMs(double averageDurationMs) { this.averageDurationMs = averageDurationMs; }
    }

    public static class QualityGateMetricsDto {
        private double passedTotal;
        private double failedTotal;
        private double notEvaluatedTotal;

        public QualityGateMetricsDto() {}

        public QualityGateMetricsDto(double passedTotal, double failedTotal, double notEvaluatedTotal) {
            this.passedTotal = passedTotal;
            this.failedTotal = failedTotal;
            this.notEvaluatedTotal = notEvaluatedTotal;
        }

        public double getPassedTotal() { return passedTotal; }
        public void setPassedTotal(double passedTotal) { this.passedTotal = passedTotal; }
        public double getFailedTotal() { return failedTotal; }
        public void setFailedTotal(double failedTotal) { this.failedTotal = failedTotal; }
        public double getNotEvaluatedTotal() { return notEvaluatedTotal; }
        public void setNotEvaluatedTotal(double notEvaluatedTotal) { this.notEvaluatedTotal = notEvaluatedTotal; }
    }

    public static class SystemMetricsDto {
        private double uptimeSeconds;
        private Instant timestamp;

        public SystemMetricsDto() {}

        public SystemMetricsDto(double uptimeSeconds, Instant timestamp) {
            this.uptimeSeconds = uptimeSeconds;
            this.timestamp = timestamp;
        }

        public double getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(double uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
}
