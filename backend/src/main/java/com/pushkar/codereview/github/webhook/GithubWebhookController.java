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

    public GithubWebhookController(GithubWebhookService webhookService,
                                   GithubWebhookSignatureVerifier signatureVerifier) {
        this.webhookService = webhookService;
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryHeader,
            @RequestBody(required = false) byte[] payloadBytes) {

        // 1. Validate Signature Header & Configuration
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook rejected: Missing X-Hub-Signature-256 header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Missing X-Hub-Signature-256 header"
            ));
        }

        if (signatureVerifier != null && !signatureVerifier.isSecretConfigured()) {
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
            log.warn("Webhook rejected: Invalid signature for deliveryId={}", deliveryHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Invalid webhook signature"
            ));
        }

        // 4. Process Event
        GithubWebhookResponse response = webhookService.processWebhook(deliveryHeader, eventHeader, payloadBytes);

        if ("ACCEPTED".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
