package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "github_webhook_deliveries")
public class GithubWebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "installation_id")
    private Long installationId;

    @Column(name = "delivery_id", nullable = false, unique = true, length = 128)
    private String deliveryId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "repository", length = 255)
    private String repository;

    @Column(name = "pull_request_number")
    private Integer pullRequestNumber;

    @Column(name = "commit_sha", length = 128)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WebhookDeliveryStatus status = WebhookDeliveryStatus.PROCESSING;

    @Column(name = "code_review_id")
    private Long codeReviewId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "error_category", length = 64)
    private String errorCategory;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retryable", nullable = false)
    private boolean retryable = false;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public GithubWebhookDelivery() {
    }

    public GithubWebhookDelivery(String deliveryId, String eventType) {
        this(deliveryId, eventType, null, null, null, null);
    }

    public GithubWebhookDelivery(String deliveryId,
                                 String eventType,
                                 String action,
                                 String repository,
                                 Integer pullRequestNumber,
                                 String commitSha) {
        this(deliveryId, eventType, action, repository, pullRequestNumber, commitSha, null, null);
    }

    public GithubWebhookDelivery(String deliveryId,
                                 String eventType,
                                 String action,
                                 String repository,
                                 Integer pullRequestNumber,
                                 String commitSha,
                                 Long installationId,
                                 User user) {
        this.deliveryId = deliveryId;
        this.eventType = eventType;
        this.action = action;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
        this.installationId = installationId;
        this.user = user;
        this.status = WebhookDeliveryStatus.PROCESSING;
        this.attemptCount = 1;
        this.maxAttempts = 3;
        this.retryable = false;
        this.receivedAt = Instant.now();
        this.startedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
        if (this.startedAt == null) {
            this.startedAt = this.receivedAt;
        }
    }

    public void markCompleted(Long reviewId) {
        this.status = WebhookDeliveryStatus.COMPLETED;
        this.codeReviewId = reviewId;
        this.retryable = false;
        this.errorCategory = null;
        this.errorMessage = null;
        this.nextRetryAt = null;
        this.processedAt = Instant.now();
    }

    public void markIgnored(String reason) {
        this.status = WebhookDeliveryStatus.IGNORED;
        this.errorMessage = reason;
        this.retryable = false;
        this.nextRetryAt = null;
        this.processedAt = Instant.now();
    }

    public void markFailed(String category, String message, boolean isTransient, Integer backoffSeconds) {
        this.status = WebhookDeliveryStatus.FAILED;
        this.errorCategory = category;
        this.errorMessage = (message != null && message.length() > 1000) ? message.substring(0, 997) + "..." : message;
        this.processedAt = Instant.now();

        if (isTransient && this.attemptCount < this.maxAttempts) {
            this.retryable = true;
            int delay = (backoffSeconds != null && backoffSeconds > 0) ? backoffSeconds : calculateBackoffSeconds();
            this.nextRetryAt = Instant.now().plusSeconds(delay);
        } else {
            this.retryable = false;
            this.nextRetryAt = null;
        }
    }

    public void markClaimedForRetry() {
        this.status = WebhookDeliveryStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.attemptCount++;
        this.nextRetryAt = null;
    }

    public int calculateBackoffSeconds() {
        return (int) Math.min(300, Math.pow(2, Math.max(1, this.attemptCount)) * 15);
    }

    public boolean isEligibleForRetry() {
        if (this.status == WebhookDeliveryStatus.COMPLETED || this.status == WebhookDeliveryStatus.IGNORED) {
            return false;
        }
        if (this.status == WebhookDeliveryStatus.PROCESSING) {
            // Processing is only eligible if lease is stale (> 5 minutes)
            Instant threshold = Instant.now().minusSeconds(300);
            return (this.startedAt != null && this.startedAt.isBefore(threshold))
                    || (this.receivedAt != null && this.receivedAt.isBefore(threshold));
        }
        return this.status == WebhookDeliveryStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
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

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubWebhookDelivery that = (GithubWebhookDelivery) o;
        return Objects.equals(deliveryId, that.deliveryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId);
    }
}
