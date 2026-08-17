package com.pushkar.codereview.config.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class CustomHealthIndicatorTest {

    @Test
    void testGeminiAiHealthIndicator_ValidKey_ReturnsUp() {
        GeminiAiHealthIndicator healthIndicator = new GeminiAiHealthIndicator(null, "valid-gemini-key-123");

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("service", "Gemini AI Engine");
        assertThat(health.getDetails()).containsEntry("status", "CONFIGURED");
        assertThat(health.getDetails()).doesNotContainValue("valid-gemini-key-123");
    }

    @Test
    void testGeminiAiHealthIndicator_MissingKey_ReturnsDown() {
        GeminiAiHealthIndicator healthIndicator = new GeminiAiHealthIndicator(null, "");

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "NOT_CONFIGURED");
    }

    @Test
    void testGithubApiHealthIndicator_ValidCredentials_ReturnsUp() {
        GithubApiHealthIndicator healthIndicator = new GithubApiHealthIndicator("123456", "-----BEGIN RSA PRIVATE KEY-----");

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("service", "GitHub API Integration");
        assertThat(health.getDetails()).containsEntry("status", "CONFIGURED");
        assertThat(health.getDetails()).doesNotContainValue("-----BEGIN RSA PRIVATE KEY-----");
    }

    @Test
    void testGithubApiHealthIndicator_MissingCredentials_ReturnsDown() {
        GithubApiHealthIndicator healthIndicator = new GithubApiHealthIndicator("", "");

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "NOT_CONFIGURED");
    }
}
