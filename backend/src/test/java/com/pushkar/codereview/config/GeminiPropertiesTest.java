package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(GeminiProperties.class)
@ContextConfiguration(classes = GeminiProperties.class)
@TestPropertySource(properties = {
    "gemini.api-key=test-gemini-key-12345",
    "gemini.model=gemini-2.5-pro",
    "gemini.api-base-url=https://custom-gemini-host.com"
})
class GeminiPropertiesTest {

    @Autowired
    private GeminiProperties geminiProperties;

    @Test
    void testGeminiPropertiesPropertyBindingAndOverrides() {
        assertThat(geminiProperties).isNotNull();
        assertThat(geminiProperties.getApiKey()).isEqualTo("test-gemini-key-12345");
        assertThat(geminiProperties.getModel()).isEqualTo("gemini-2.5-pro");
        assertThat(geminiProperties.getApiBaseUrl()).isEqualTo("https://custom-gemini-host.com");
    }

    @Test
    void testGeminiPropertiesDefaults() {
        GeminiProperties defaults = new GeminiProperties();
        assertThat(defaults.getModel()).isEqualTo("gemini-2.5-flash");
        assertThat(defaults.getApiBaseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
        assertThat(defaults.getApiKey()).isNull();
    }
}
