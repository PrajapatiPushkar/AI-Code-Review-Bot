package com.pushkar.codereview.github.webhook.dto;

import com.pushkar.codereview.github.webhook.WebhookDeliveryStatus;

import java.time.Instant;

public class WebhookDeliveryItemResponse {

    private Long id;
    private String deliveryId;
    private String eventType;
    private String action;
    private String repository;
    private Integer pullRequestNumber;
    private String commitSha;
    private WebhookDeliveryStatus status;
    private Long codeReviewId;
    private int attemptCount;
    private int maxAttempts;
    private boolean retryable;
    private boolean retryEligible;
    private String errorCategory;
    private String errorMessage;
    private Instant receivedAt;
    private Instant startedAt;
    private Instant processedAt;
    private Instant nextRetryAt;

    public WebhookDeliveryItemResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Integer pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public WebhookDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookDeliveryStatus status) {
        this.status = status;
    }

    public Long getCodeReviewId() {
        return codeReviewId;
    }

    public void setCodeReviewId(Long codeReviewId) {
        this.codeReviewId = codeReviewId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryEligible() {
        return retryEligible;
    }

    public void setRetryEligible(boolean retryEligible) {
        this.retryEligible = retryEligible;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}
