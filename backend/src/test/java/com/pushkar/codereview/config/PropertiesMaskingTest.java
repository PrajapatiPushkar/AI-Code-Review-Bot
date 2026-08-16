package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesMaskingTest {

    @Test
    void testGithubProperties_ToString_MasksSecrets() {
        GithubProperties properties = new GithubProperties();
        properties.setAppId("123456");
        properties.setPrivateKey("superSecretPrivateKeyContent");
        properties.setWebhookSecret("superSecretWebhookSecretContent");

        String str = properties.toString();

        assertThat(str).contains("[PROTECTED]");
        assertThat(str).doesNotContain("superSecretPrivateKeyContent");
        assertThat(str).doesNotContain("superSecretWebhookSecretContent");
    }

    @Test
    void testGeminiProperties_ToString_MasksSecrets() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("secretGeminiApiKey123");

        String str = properties.toString();

        assertThat(str).contains("[PROTECTED]");
        assertThat(str).doesNotContain("secretGeminiApiKey123");
    }

    @Test
    void testJwtProperties_ToString_MasksSecrets() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("superSecretJwtKeyThatMustBeProtected");

        String str = properties.toString();

        assertThat(str).contains("[PROTECTED]");
        assertThat(str).doesNotContain("superSecretJwtKeyThatMustBeProtected");
    }
}
