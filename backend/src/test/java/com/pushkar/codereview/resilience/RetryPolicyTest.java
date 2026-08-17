package com.pushkar.codereview.resilience;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.config.resilience.ResilienceExecutor;
import com.pushkar.codereview.config.resilience.ResilienceProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryPolicyTest {

    private ResilienceProperties properties;
    private CodeReviewMetrics metrics;
    private ResilienceExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new ResilienceProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialIntervalMs(10);
        properties.getRetry().setMultiplier(1.0);
        properties.getRetry().setMaxIntervalMs(50);

        metrics = new CodeReviewMetrics(new SimpleMeterRegistry());
        executor = new ResilienceExecutor(properties, metrics);
    }

    @Test
    void testTransientFailure_SucceedsOnSecondAttempt() {
        AtomicInteger callCount = new AtomicInteger(0);

        String result = executor.executeSupplier("github", () -> {
            int count = callCount.incrementAndGet();
            if (count == 1) {
                throw new GithubApiException("Rate limited", 429);
            }
            return "SUCCESS";
        });

        assertThat(result).isEqualTo("SUCCESS");
        assertThat(callCount.get()).isEqualTo(2);
        assertThat(metrics.getRetryCounter("github").count()).isEqualTo(1.0);
    }

    @Test
    void testTransientFailure_ExhaustsRetriesAndThrowsException() {
        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() -> executor.executeSupplier("github", () -> {
            callCount.incrementAndGet();
            throw new GithubApiException("GitHub Server Error", 502);
        }))
        .isInstanceOf(GithubApiException.class)
        .hasMessageContaining("GitHub Server Error");

        assertThat(callCount.get()).isEqualTo(3);
        assertThat(metrics.getRetryCounter("github").count()).isEqualTo(2.0);
        assertThat(metrics.getExternalFailureCounter("github").count()).isEqualTo(1.0);
    }

    @Test
    void testNonRetryableException_FailsImmediatelyWithoutRetry() {
        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() -> executor.executeSupplier("github", () -> {
            callCount.incrementAndGet();
            throw new ResourceNotFoundException("Repository not found");
        }))
        .isInstanceOf(ResourceNotFoundException.class);

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(metrics.getRetryCounter("github")).isNull();
    }

    @Test
    void testUnauthorizedException_FailsImmediatelyWithoutRetry() {
        AtomicInteger callCount = new AtomicInteger(0);

        assertThatThrownBy(() -> executor.executeSupplier("github", () -> {
            callCount.incrementAndGet();
            throw new GithubApiException("Unauthorized access", 401);
        }))
        .isInstanceOf(GithubApiException.class);

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(metrics.getRetryCounter("github")).isNull();
    }
}
