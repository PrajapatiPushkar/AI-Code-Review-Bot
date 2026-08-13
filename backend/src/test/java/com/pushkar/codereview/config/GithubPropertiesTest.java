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
@EnableConfigurationProperties(GithubProperties.class)
@ContextConfiguration(classes = GithubProperties.class)
@TestPropertySource(properties = {
    "github.app-id=123456",
    "github.app-name=test-bot",
    "github.private-key=test-private-key",
    "github.webhook-secret=test-webhook-secret",
    "github.api-base-url=https://api.github.com"
})
class GithubPropertiesTest {

    @Autowired
    private GithubProperties githubProperties;

    @Test
    void testGithubPropertiesLoad() {
        assertThat(githubProperties).isNotNull();
        assertThat(githubProperties.getAppId()).isEqualTo("123456");
        assertThat(githubProperties.getAppName()).isEqualTo("test-bot");
        assertThat(githubProperties.getPrivateKey()).isEqualTo("test-private-key");
        assertThat(githubProperties.getWebhookSecret()).isEqualTo("test-webhook-secret");
        assertThat(githubProperties.getApiBaseUrl()).isEqualTo("https://api.github.com");
    }
}
