package com.pushkar.codereview.github.webhook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GithubWebhookResponse {

    private String deliveryId;
    private String status;
    private Long reviewId;
    private Boolean initiated;
    private String message;
    private Instant timestamp;

    public GithubWebhookResponse() {
        this.timestamp = Instant.now();
    }

    public GithubWebhookResponse(String deliveryId, String status, Long reviewId, Boolean initiated, String message) {
        this.deliveryId = deliveryId;
        this.status = status;
        this.reviewId = reviewId;
        this.initiated = initiated;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static GithubWebhookResponse accepted(String deliveryId, Long reviewId, String message) {
        return new GithubWebhookResponse(deliveryId, "ACCEPTED", reviewId, true, message);
    }

    public static GithubWebhookResponse duplicate(String deliveryId, Long reviewId, String message) {
        return new GithubWebhookResponse(deliveryId, "DUPLICATE", reviewId, false, message);
    }

    public static GithubWebhookResponse ignored(String deliveryId, String message) {
        return new GithubWebhookResponse(deliveryId, "IGNORED", null, false, message);
    }

    public static GithubWebhookResponse pong(String deliveryId, String message) {
        return new GithubWebhookResponse(deliveryId, "PONG", null, false, message);
    }

    public static GithubWebhookResponse failed(String deliveryId, String message) {
        return new GithubWebhookResponse(deliveryId, "FAILED", null, false, message);
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Boolean getInitiated() {
        return initiated;
    }

    public void setInitiated(Boolean initiated) {
        this.initiated = initiated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
