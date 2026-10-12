package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/webhooks/github", "/api/v1/webhooks/github"})
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);

    private final GithubWebhookService webhookService;
    private final GithubWebhookSignatureVerifier signatureVerifier;
    private final com.pushkar.codereview.config.WebhookMetrics webhookMetrics;

    public GithubWebhookController(GithubWebhookService webhookService,
                                   GithubWebhookSignatureVerifier signatureVerifier) {
        this(webhookService, signatureVerifier, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GithubWebhookController(GithubWebhookService webhookService,
                                   GithubWebhookSignatureVerifier signatureVerifier,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false) com.pushkar.codereview.config.WebhookMetrics webhookMetrics) {
        this.webhookService = webhookService;
        this.signatureVerifier = signatureVerifier;
        this.webhookMetrics = webhookMetrics;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryHeader,
            @RequestBody(required = false) byte[] payloadBytes) {

        long startTime = System.currentTimeMillis();
        if (webhookMetrics != null) {
            webhookMetrics.recordDeliveryReceived();
        }

        try {
            // 1. Validate Signature Header & Configuration
            if (signatureHeader == null || signatureHeader.isBlank()) {
                if (webhookMetrics != null) {
                    webhookMetrics.recordSignatureVerification(false);
                }
                log.warn("Webhook rejected: Missing X-Hub-Signature-256 header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED.value(),
                        "error", "Unauthorized",
                        "message", "Missing X-Hub-Signature-256 header"
                ));
            }

            if (signatureVerifier != null && !signatureVerifier.isSecretConfigured()) {
                if (webhookMetrics != null) {
                    webhookMetrics.recordSignatureVerification(false);
                }
                log.error("Webhook rejected: Webhook secret is not configured");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED.value(),
                        "error", "Unauthorized",
                        "message", "Webhook secret is not configured"
                ));
            }

            // 2. Validate Required Headers
            if (eventHeader == null || eventHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", "Missing required X-GitHub-Event header"
                ));
            }

            if (deliveryHeader == null || deliveryHeader.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", "Missing required X-GitHub-Delivery header"
                ));
            }

            if (payloadBytes == null || payloadBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", "Missing or empty request body"
                ));
            }

            // 3. Verify Signature
            if (signatureVerifier != null && !signatureVerifier.verifySignature(payloadBytes, signatureHeader)) {
                if (webhookMetrics != null) {
                    webhookMetrics.recordSignatureVerification(false);
                }
                log.warn("Webhook rejected: Invalid signature for deliveryId={}", deliveryHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", HttpStatus.UNAUTHORIZED.value(),
                        "error", "Unauthorized",
                        "message", "Invalid webhook signature"
                ));
            }

            if (webhookMetrics != null) {
                webhookMetrics.recordSignatureVerification(true);
            }

            // 4. Process Event
            GithubWebhookResponse response = webhookService.processWebhook(deliveryHeader, eventHeader, payloadBytes);

            if ("ACCEPTED".equalsIgnoreCase(response.getStatus())) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            return ResponseEntity.ok(response);
        } finally {
            if (webhookMetrics != null) {
                webhookMetrics.recordProcessingTime(System.currentTimeMillis() - startTime);
            }
        }
    }
}
