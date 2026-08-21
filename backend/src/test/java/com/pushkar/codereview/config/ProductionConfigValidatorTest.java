package com.pushkar.codereview.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigValidatorTest {

    private JwtProperties jwtProperties;
    private GithubProperties githubProperties;
    private GeminiProperties geminiProperties;
    private MockEnvironment environment;
    private ProductionConfigValidator validator;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties("prodSecretKeyThatIsAtLeast32BytesLongForSecurity9999", 3600000L, "Bearer", "ai-code-review-bot");
        githubProperties = new GithubProperties();
        githubProperties.setAppId("123456");
        githubProperties.setPrivateKey("-----BEGIN RSA PRIVATE KEY-----\ndummyKey\n-----END RSA PRIVATE KEY-----");

        geminiProperties = new GeminiProperties("realGeminiApiKey123", "gemini-3.6-flash", "https://generativelanguage.googleapis.com");

        environment = new MockEnvironment();
        environment.setProperty("spring.datasource.password", "secretDbPassword");

        validator = new ProductionConfigValidator(jwtProperties, githubProperties, geminiProperties, environment);
    }

    @Test
    void testRun_ValidProductionConfig_Success() {
        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void testRun_DefaultDevJwtSecret_ThrowsException() {
        jwtProperties.setSecret("defaultDevSecretKeyThatIsAtLeast32BytesLongForHMACSHA256Signatures12345");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be configured with a secure production secret");
    }

    @Test
    void testRun_MissingGeminiApiKey_ThrowsException() {
        geminiProperties.setApiKey(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY must not be blank in production");
    }

    @Test
    void testRun_MissingGithubAppId_ThrowsException() {
        githubProperties.setAppId(" ");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GITHUB_APP_ID must not be blank in production");
    }

    @Test
    void testRun_MissingGithubPrivateKey_ThrowsException() {
        githubProperties.setPrivateKey(null);
        githubProperties.setPrivateKeyPath(null);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GITHUB_PRIVATE_KEY or GITHUB_PRIVATE_KEY_PATH must be configured in production");
    }

    @Test
    void testRun_MissingDatabasePassword_ThrowsException() {
        environment.setProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Database password");
    }
}
