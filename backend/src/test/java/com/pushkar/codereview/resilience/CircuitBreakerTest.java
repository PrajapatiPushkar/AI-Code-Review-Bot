package com.pushkar.codereview.resilience;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.config.resilience.ResilienceExecutor;
import com.pushkar.codereview.config.resilience.ResilienceProperties;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.exception.GithubApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerTest {

    private ResilienceProperties properties;
    private CodeReviewMetrics metrics;
    private ResilienceExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new ResilienceProperties();
        properties.getRetry().setMaxAttempts(1); // Single attempt for quick circuit breaker testing
        properties.getCircuitBreaker().setFailureRateThreshold(50.0f);
        properties.getCircuitBreaker().setSlidingWindowSize(4);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(4);
        properties.getCircuitBreaker().setWaitDurationInOpenStateMs(5000);

        metrics = new CodeReviewMetrics(new SimpleMeterRegistry());
        executor = new ResilienceExecutor(properties, metrics);
    }

    @Test
    void testCircuitBreaker_OpensAfterFailuresAndFailsFast() {
        // Trigger failures to breach failure threshold
        for (int i = 0; i < 4; i++) {
            try {
                executor.executeSupplier("gemini", () -> {
                    throw new GeminiAiReviewException("Gemini Service Unavailable");
                });
            } catch (Exception ignored) {
            }
        }

        CircuitBreaker cb = executor.getCircuitBreaker("gemini");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call must fail fast without executing logic
        assertThatThrownBy(() -> executor.executeSupplier("gemini", () -> "SHOULD_NOT_EXECUTE"))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("circuit breaker is OPEN");

        assertThat(metrics.getCircuitBreakerOpenCounter("gemini").count()).isEqualTo(1.0);
    }

    @Test
    void testCircuitBreaker_GithubDependency_OpensAndThrowsGithubApiException() {
        for (int i = 0; i < 4; i++) {
            try {
                executor.executeSupplier("github", () -> {
                    throw new GithubApiException("GitHub Server Error", 503);
                });
            } catch (Exception ignored) {
            }
        }

        CircuitBreaker cb = executor.getCircuitBreaker("github");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> executor.executeSupplier("github", () -> "SHOULD_NOT_EXECUTE"))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("circuit breaker is OPEN");

        assertThat(metrics.getCircuitBreakerOpenCounter("github").count()).isEqualTo(1.0);
    }
}
