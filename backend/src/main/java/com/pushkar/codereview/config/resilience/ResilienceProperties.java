package com.pushkar.codereview.config.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resilience")
public class ResilienceProperties {

    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private Timeouts timeouts = new Timeouts();

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public Timeouts getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialIntervalMs = 500;
        private double multiplier = 2.0;
        private long maxIntervalMs = 2000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxIntervalMs() {
            return maxIntervalMs;
        }

        public void setMaxIntervalMs(long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
        }
    }

    public static class CircuitBreaker {
        private float failureRateThreshold = 50.0f;
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private long waitDurationInOpenStateMs = 10000;
        private int permittedNumberOfCallsInHalfOpenState = 3;

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public long getWaitDurationInOpenStateMs() {
            return waitDurationInOpenStateMs;
        }

        public void setWaitDurationInOpenStateMs(long waitDurationInOpenStateMs) {
            this.waitDurationInOpenStateMs = waitDurationInOpenStateMs;
        }

        public int getPermittedNumberOfCallsInHalfOpenState() {
            return permittedNumberOfCallsInHalfOpenState;
        }

        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
        }
    }

    public static class Timeouts {
        private int githubConnectTimeoutMs = 5000;
        private int githubReadTimeoutMs = 15000;
        private int geminiConnectTimeoutMs = 5000;
        private int geminiReadTimeoutMs = 30000;

        public int getGithubConnectTimeoutMs() {
            return githubConnectTimeoutMs;
        }

        public void setGithubConnectTimeoutMs(int githubConnectTimeoutMs) {
            this.githubConnectTimeoutMs = githubConnectTimeoutMs;
        }

        public int getGithubReadTimeoutMs() {
            return githubReadTimeoutMs;
        }

        public void setGithubReadTimeoutMs(int githubReadTimeoutMs) {
            this.githubReadTimeoutMs = githubReadTimeoutMs;
        }

        public int getGeminiConnectTimeoutMs() {
            return geminiConnectTimeoutMs;
        }

        public void setGeminiConnectTimeoutMs(int geminiConnectTimeoutMs) {
            this.geminiConnectTimeoutMs = geminiConnectTimeoutMs;
        }

        public int getGeminiReadTimeoutMs() {
            return geminiReadTimeoutMs;
        }

        public void setGeminiReadTimeoutMs(int geminiReadTimeoutMs) {
            this.geminiReadTimeoutMs = geminiReadTimeoutMs;
        }
    }
}
