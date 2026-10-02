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
                .automaticTransitionFromOpenToHalfOpenEnabled(cbProps.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .recordException(this::isTransientException)
                .ignoreException(e -> !isTransientException(e))
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
                String reviewId = MDC.get("reviewId");
                Throwable lastThrowable = event.getLastThrowable();
                String httpStatus = extractHttpStatus(lastThrowable);
                String exceptionType = lastThrowable != null ? lastThrowable.getClass().getSimpleName() : "Unknown";
                log.warn("Retrying external call attempt {}/{} for dependency={} [reviewId={}, correlationId={}, status={}, exceptionType={}]: {}",
                        attempt, properties.getRetry().getMaxAttempts(), dep, reviewId, correlationId,
                        httpStatus, exceptionType,
                        lastThrowable != null ? lastThrowable.getMessage() : "Transient error");

                if (codeReviewMetrics != null) {
                    codeReviewMetrics.recordRetry(dep);
                }
            });
            return r;
        });

        Supplier<T> retryDecorated = Retry.decorateSupplier(retry, supplier);
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, retryDecorated);

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            String correlationId = MDC.get("correlationId");
            String reviewId = MDC.get("reviewId");
            String cbState = circuitBreaker.getState().name();
            log.warn("Request rejected by open circuit breaker for dependency={} [reviewId={}, correlationId={}, cbState={}, status=503, exceptionType={}]: {}",
                    dep, reviewId, correlationId, cbState, e.getClass().getSimpleName(), e.getMessage());

            if (codeReviewMetrics != null) {
                codeReviewMetrics.recordCircuitBreakerOpen(dep);
            }

            if ("gemini".equalsIgnoreCase(dep)) {
                throw new GeminiAiReviewException("Gemini AI circuit breaker is OPEN. Dependency is currently unavailable.", 503, e);
            } else {
                throw new GithubApiException("GitHub API circuit breaker is OPEN. Dependency is currently unavailable.", 503, e);
            }
        } catch (Exception e) {
            String correlationId = MDC.get("correlationId");
            String reviewId = MDC.get("reviewId");
            String cbState = circuitBreaker.getState().name();
            String httpStatus = extractHttpStatus(e);
            if (isTransientException(e)) {
                if (codeReviewMetrics != null) {
                    codeReviewMetrics.recordExternalFailure(dep);
                }
                log.error("External call failed after retries for dependency={} [reviewId={}, correlationId={}, cbState={}, status={}, exceptionType={}]: {}",
                        dep, reviewId, correlationId, cbState, httpStatus, e.getClass().getSimpleName(), e.getMessage());
            } else {
                log.warn("Non-retryable external call failure for dependency={} [reviewId={}, correlationId={}, cbState={}, status={}, exceptionType={}]: {}",
                        dep, reviewId, correlationId, cbState, httpStatus, e.getClass().getSimpleName(), e.getMessage());
            }
            throw e;
        }
    }

    public boolean isTransientException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        if (throwable instanceof CallNotPermittedException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof AccessDeniedException
                || throwable instanceof ResourceNotFoundException
                || throwable instanceof GithubInstallationVerificationException) {
            return false;
        }

        if (throwable instanceof GithubApiException gae) {
            int statusCode = gae.getStatusCode();
            return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode >= 500;
        }

        if (throwable instanceof GeminiAiReviewException gare) {
            if (gare.getStatusCode() != null) {
                int status = gare.getStatusCode();
                return status == 429 || status == 502 || status == 503 || status == 504 || status >= 500;
            }
            if (gare.getCause() != null && gare.getCause() != gare) {
                return isTransientException(gare.getCause());
            }
            String msg = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
            if (msg.contains("missing") || msg.contains("invalid") || msg.contains("not configured")
                    || msg.contains("candidate-less") || msg.contains("empty") || msg.contains("failed to parse")
                    || msg.contains("null response") || msg.contains("status code: 4")) {
                return false;
            }
            if (msg.contains("status code: 5") || msg.contains("status code: 429") || msg.contains("timeout") || msg.contains("unavailable") || msg.contains("high demand")) {
                return true;
            }
            return false;
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

    private String extractHttpStatus(Throwable t) {
        if (t == null) {
            return "N/A";
        }
        if (t instanceof GithubApiException gae) {
            return String.valueOf(gae.getStatusCode());
        }
        if (t instanceof GeminiAiReviewException gare && gare.getStatusCode() != null) {
            return String.valueOf(gare.getStatusCode());
        }
        if (t instanceof org.springframework.web.client.HttpStatusCodeException hsce) {
            return String.valueOf(hsce.getStatusCode().value());
        }
        if (t.getCause() != null && t.getCause() != t) {
            return extractHttpStatus(t.getCause());
        }
        return "N/A";
    }

    public CircuitBreaker getCircuitBreaker(String dependencyName) {
        return circuitBreakers.get(dependencyName.toLowerCase());
    }

    public Retry getRetry(String dependencyName) {
        return retries.get(dependencyName.toLowerCase());
    }
}
