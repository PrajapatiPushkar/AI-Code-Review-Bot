package com.pushkar.codereview.github.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "webhook.retry.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDeliveryRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryRetryScheduler.class);

    private final GithubWebhookService webhookService;

    public WebhookDeliveryRetryScheduler(GithubWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Scheduled(fixedDelayString = "${webhook.retry.scheduler.fixed-delay-ms:60000}", initialDelay = 10000)
    public void runScheduledRecoveryAndRetries() {
        try {
            int recovered = webhookService.recoverStaleProcessingDeliveries(300);
            if (recovered > 0) {
                log.info("Webhook scheduler recovered {} stale processing deliveries.", recovered);
            }

            int retried = webhookService.processPendingRetries();
            if (retried > 0) {
                log.info("Webhook scheduler processed {} pending retries.", retried);
            }
        } catch (Exception e) {
            log.error("Error occurred during scheduled webhook recovery and retry job: {}", e.getMessage(), e);
        }
    }
}
