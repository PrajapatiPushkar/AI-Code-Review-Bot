package com.pushkar.codereview.github.webhook;

public enum WebhookDeliveryStatus {
    PROCESSING,
    COMPLETED,
    IGNORED,
    FAILED,
    DUPLICATE
}
