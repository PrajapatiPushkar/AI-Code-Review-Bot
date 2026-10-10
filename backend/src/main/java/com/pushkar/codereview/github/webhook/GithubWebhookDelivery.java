package com.pushkar.codereview.github.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

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
        this.deliveryId = deliveryId;
        this.eventType = eventType;
        this.action = action;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
        this.status = WebhookDeliveryStatus.PROCESSING;
        this.receivedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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
