package com.pushkar.codereview.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Metric instrumentation for GitHub Webhook delivery, signature verification,
 * deduplication, retries, and scheduled recovery pipelines.
 * All metrics use bounded, low-cardinality tags.
 */
@Component
public class WebhookMetrics {

    private final MeterRegistry meterRegistry;

    // Delivery lifecycle counters
    private final Counter deliveriesReceivedCounter;
    private final Counter duplicateDeliveriesCounter;
    private final Counter retryAttemptsCounter;
    private final Counter retriesExhaustedCounter;
    private final Counter staleRecoveredCounter;
    private final Counter retriesProcessedCounter;

    // Signature verification (tag: result=valid|invalid)
    private final Map<String, Counter> signatureCounters = new ConcurrentHashMap<>();

    // Delivery outcome status (tag: status=ACCEPTED|IGNORED|COMPLETED|FAILED)
    private final Map<String, Counter> deliveryStatusCounters = new ConcurrentHashMap<>();

    // Webhook request processing timer
    private final Timer webhookProcessingTimer;

    public WebhookMetrics(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = (meterRegistry != null) ? meterRegistry : new SimpleMeterRegistry();

        this.deliveriesReceivedCounter = Counter.builder("webhook.deliveries.received.total")
                .description("Total number of GitHub webhook deliveries received by the endpoint")
                .register(this.meterRegistry);

        this.duplicateDeliveriesCounter = Counter.builder("webhook.deliveries.duplicate.total")
                .description("Total number of duplicate GitHub webhook deliveries detected")
                .register(this.meterRegistry);

        this.retryAttemptsCounter = Counter.builder("webhook.retries.attempts.total")
                .description("Total number of manual or scheduled webhook retry attempts initiated")
                .register(this.meterRegistry);

        this.retriesExhaustedCounter = Counter.builder("webhook.retries.exhausted.total")
                .description("Total number of webhook deliveries whose retries reached maximum attempts")
                .register(this.meterRegistry);

        this.staleRecoveredCounter = Counter.builder("webhook.recovery.stale_recovered.total")
                .description("Total number of stale PROCESSING webhook deliveries recovered by scheduler")
                .register(this.meterRegistry);

        this.retriesProcessedCounter = Counter.builder("webhook.recovery.retries_processed.total")
                .description("Total number of pending webhook retries processed by the scheduler")
                .register(this.meterRegistry);

        this.webhookProcessingTimer = Timer.builder("webhook.processing.duration")
                .description("Duration of GitHub webhook ingestion and synchronous dispatch")
                .register(this.meterRegistry);
    }

    public void recordDeliveryReceived() {
        deliveriesReceivedCounter.increment();
    }

    public void recordSignatureVerification(boolean valid) {
        String result = valid ? "valid" : "invalid";
        signatureCounters.computeIfAbsent(result, r -> Counter.builder("webhook.signatures.total")
                .tag("result", r)
                .description("Total webhook signature validations classified by outcome")
                .register(meterRegistry)).increment();
    }

    public void recordDeliveryStatus(String status) {
        String boundedStatus = (status != null && !status.isBlank())
                ? status.trim().toUpperCase()
                : "UNKNOWN";
        deliveryStatusCounters.computeIfAbsent(boundedStatus, s -> Counter.builder("webhook.deliveries.status.total")
                .tag("status", s)
                .description("Total webhook deliveries completed by final delivery state")
                .register(meterRegistry)).increment();
    }

    public void recordDuplicateDelivery() {
        duplicateDeliveriesCounter.increment();
    }

    public void recordRetryAttempt() {
        retryAttemptsCounter.increment();
    }

    public void recordRetryExhausted() {
        retriesExhaustedCounter.increment();
    }

    public void recordProcessingTime(long durationMillis) {
        webhookProcessingTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordStaleRecovered(int count) {
        if (count > 0) {
            staleRecoveredCounter.increment(count);
        }
    }

    public void recordRetriesProcessed(int count) {
        if (count > 0) {
            retriesProcessedCounter.increment(count);
        }
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public Counter getDeliveriesReceivedCounter() {
        return deliveriesReceivedCounter;
    }

    public Counter getDuplicateDeliveriesCounter() {
        return duplicateDeliveriesCounter;
    }

    public Counter getRetryAttemptsCounter() {
        return retryAttemptsCounter;
    }

    public Counter getRetriesExhaustedCounter() {
        return retriesExhaustedCounter;
    }

    public Counter getStaleRecoveredCounter() {
        return staleRecoveredCounter;
    }

    public Counter getRetriesProcessedCounter() {
        return retriesProcessedCounter;
    }

    public Counter getSignatureCounter(boolean valid) {
        return signatureCounters.get(valid ? "valid" : "invalid");
    }

    public Counter getDeliveryStatusCounter(String status) {
        return deliveryStatusCounters.get(status != null ? status.toUpperCase() : "UNKNOWN");
    }

    public Timer getWebhookProcessingTimer() {
        return webhookProcessingTimer;
    }
}
