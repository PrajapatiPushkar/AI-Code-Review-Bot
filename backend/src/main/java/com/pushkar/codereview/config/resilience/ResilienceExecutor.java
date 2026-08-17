package com.pushkar.codereview.config.resilience;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ResilienceExecutor {

    private static final Logger log = LoggerFactory.getLogger(ResilienceExecutor.class);

    private final ResilienceProperties properties;
    private final CodeReviewMetrics codeReviewMetrics;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();

    @Autowired
    public ResilienceExecutor(ResilienceProperties properties,
                              @Autowired(required = false) CodeReviewMetrics codeReviewMetrics) {
        this.properties = properties;
        this.codeReviewMetrics = codeReviewMetrics;

        ResilienceProperties.Retry retryProps = properties.getRetry();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryProps.getMaxAttempts())
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                        retryProps.getInitialIntervalMs(),
                        retryProps.getMultiplier(),
                        retryProps.getMaxIntervalMs()
                ))
                .retryOnException(this::isTransientException)
                .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);

        ResilienceProperties.CircuitBreaker cbProps = properties.getCircuitBreaker();
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .slidingWindowSize(cbProps.getSlidingWindowSize())
                .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(cbProps.getWaitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(cbProps.getPermittedNumberOfCallsInHalfOpenState())
                .recordException(this::isTransientException)
                .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);
    }

    public <T> T executeSupplier(String dependencyName, Supplier<T> supplier) {
        String dep = (dependencyName != null && !dependencyName.isBlank()) ? dependencyName.toLowerCase() : "unknown";

        CircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(dep, circuitBreakerRegistry::circuitBreaker);
        Retry retry = retries.computeIfAbsent(dep, k -> {
            Retry r = retryRegistry.retry(k);
            r.getEventPublisher().onRetry(event -> {
                int attempt = event.getNumberOfRetryAttempts();
                String correlationId = MDC.get("correlationId");
                log.warn("Retrying external call attempt {}/{} for dependency={} [correlationId={}]: {}",
                        attempt, properties.getRetry().getMaxAttempts(), dep, correlationId,
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "Transient error");

                if (codeReviewMetrics != null) {
                    codeReviewMetrics.recordRetry(dep);
                }
            });
            return r;
        });

        Supplier<T> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, supplier));

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            String correlationId = MDC.get("correlationId");
            log.warn("Request rejected by open circuit breaker for dependency={} [correlationId={}]", dep, correlationId);

            if (codeReviewMetrics != null) {
                codeReviewMetrics.recordCircuitBreakerOpen(dep);
            }

            if ("gemini".equalsIgnoreCase(dep)) {
                throw new GeminiAiReviewException("Gemini AI circuit breaker is OPEN. Dependency is currently unavailable.", e);
            } else {
                throw new GithubApiException("GitHub API circuit breaker is OPEN. Dependency is currently unavailable.", 503, e);
            }
        } catch (Exception e) {
            if (isTransientException(e)) {
                if (codeReviewMetrics != null) {
                    codeReviewMetrics.recordExternalFailure(dep);
                }
                String correlationId = MDC.get("correlationId");
                log.error("External call failed after retries for dependency={} [correlationId={}]: {}",
                        dep, correlationId, e.getMessage());
            }
            throw e;
        }
    }

    public boolean isTransientException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        if (throwable instanceof IllegalArgumentException
                || throwable instanceof AccessDeniedException
                || throwable instanceof ResourceNotFoundException
                || throwable instanceof GithubInstallationVerificationException) {
            return false;
        }

        if (throwable instanceof GithubApiException gae) {
            int statusCode = gae.getStatusCode();
            return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode >= 500;
        }

        if (throwable instanceof GeminiAiReviewException) {
            String msg = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
            if (msg.contains("missing") || msg.contains("invalid") || msg.contains("not configured")) {
                return false;
            }
            return true;
        }

        if (throwable instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
            int status = httpEx.getStatusCode().value();
            return status == 429 || status == 502 || status == 503 || status == 504 || status >= 500;
        }

        if (throwable instanceof org.springframework.web.client.ResourceAccessException
                || throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.ConnectException
                || throwable instanceof IOException) {
            return true;
        }

        return false;
    }

    public CircuitBreaker getCircuitBreaker(String dependencyName) {
        return circuitBreakers.get(dependencyName.toLowerCase());
    }

    public Retry getRetry(String dependencyName) {
        return retries.get(dependencyName.toLowerCase());
    }
}
