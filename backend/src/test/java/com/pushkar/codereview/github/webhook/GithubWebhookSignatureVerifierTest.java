package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.config.GithubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubWebhookSignatureVerifierTest {

    private GithubProperties githubProperties;
    private GithubWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        githubProperties = new GithubProperties();
        githubProperties.setWebhookSecret("my-super-secret-key");
        verifier = new GithubWebhookSignatureVerifier(githubProperties);
    }

    @Test
    void testVerifySignature_ValidSignature_ReturnsTrue() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.computeSignature(payload);

        assertThat(verifier.verifySignature(payload, signature)).isTrue();
    }

    @Test
    void testVerifySignature_InvalidSignature_ReturnsFalse() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String invalidSignature = "sha256=1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

        assertThat(verifier.verifySignature(payload, invalidSignature)).isFalse();
    }

    @Test
    void testVerifySignature_MismatchedPayload_ReturnsFalse() {
        byte[] payload1 = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.computeSignature(payload1);

        assertThat(verifier.verifySignature(payload2, signature)).isFalse();
    }

    @Test
    void testVerifySignature_MissingPrefix_ReturnsFalse() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signatureWithoutPrefix = "1234567890abcdef";

        assertThat(verifier.verifySignature(payload, signatureWithoutPrefix)).isFalse();
    }

    @Test
    void testVerifySignature_NullSignature_ReturnsFalse() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verifySignature(payload, null)).isFalse();
    }

    @Test
    void testVerifySignature_NullPayload_ReturnsFalse() {
        assertThat(verifier.verifySignature(null, "sha256=abcdef")).isFalse();
    }

    @Test
    void testIsSecretConfigured_WhenConfigured_ReturnsTrue() {
        assertThat(verifier.isSecretConfigured()).isTrue();
    }

    @Test
    void testIsSecretConfigured_WhenEmptyOrNull_ReturnsFalse() {
        githubProperties.setWebhookSecret("");
        assertThat(verifier.isSecretConfigured()).isFalse();

        githubProperties.setWebhookSecret(null);
        assertThat(verifier.isSecretConfigured()).isFalse();
    }

    @Test
    void testVerifySignature_WhenSecretNotConfigured_ReturnsFalse() {
        githubProperties.setWebhookSecret(null);
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verifySignature(payload, "sha256=abcdef")).isFalse();
    }

    @Test
    void testComputeSignature_WhenSecretNotConfigured_ThrowsException() {
        githubProperties.setWebhookSecret(null);
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> verifier.computeSignature(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Webhook secret is not configured");
    }
}
