package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    @Test
    void testJwtPropertiesDefaultsAndGettersSetters() {
        JwtProperties properties = new JwtProperties();

        assertThat(properties.getSecret()).isNotBlank();
        assertThat(properties.getExpirationMs()).isEqualTo(3600000L);
        assertThat(properties.getTokenType()).isEqualTo("Bearer");
        assertThat(properties.getIssuer()).isEqualTo("ai-code-review-bot");

        properties.setSecret("customSecret123456789012345678901234567890");
        properties.setExpirationMs(7200000L);
        properties.setTokenType("Bearer");
        properties.setIssuer("custom-issuer");

        assertThat(properties.getSecret()).isEqualTo("customSecret123456789012345678901234567890");
        assertThat(properties.getExpirationMs()).isEqualTo(7200000L);
        assertThat(properties.getTokenType()).isEqualTo("Bearer");
        assertThat(properties.getIssuer()).isEqualTo("custom-issuer");
    }
}
