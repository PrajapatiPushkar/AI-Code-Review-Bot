package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.config.GithubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GithubWebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookSignatureVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final GithubProperties githubProperties;

    public GithubWebhookSignatureVerifier(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public boolean isSecretConfigured() {
        String secret = getWebhookSecret();
        return secret != null && !secret.isBlank();
    }

    public boolean verifySignature(byte[] payloadBytes, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        if (payloadBytes == null) {
            return false;
        }

        String webhookSecret = getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("GitHub webhook secret is not configured. Signature verification failed.");
            return false;
        }

        try {
            String receivedHash = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim();
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] computedHashBytes = mac.doFinal(payloadBytes);
            String computedHash = HexFormat.of().formatHex(computedHashBytes);

            return MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    receivedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256 for GitHub webhook: {}", e.getMessage());
            return false;
        }
    }

    public String computeSignature(byte[] payloadBytes) {
        String webhookSecret = getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Webhook secret is not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(payloadBytes);
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }

    private String getWebhookSecret() {
        return (githubProperties != null) ? githubProperties.getWebhookSecret() : null;
    }
}
