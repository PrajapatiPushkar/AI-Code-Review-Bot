package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDeploymentSmokeVerificationTest {

    @Test
    void testFlywayMigrationsV1ToV9Exist() {
        List<String> expectedMigrations = List.of(
                "db/migration/V1__create_users_table.sql",
                "db/migration/V2__create_repositories_table.sql",
                "db/migration/V3__create_github_installations_table.sql",
                "db/migration/V4__create_code_reviews_table.sql",
                "db/migration/V5__add_user_authentication_fields.sql",
                "db/migration/V6__add_user_id_to_code_reviews.sql",
                "db/migration/V7__add_github_installation_verification.sql",
                "db/migration/V8__create_code_review_findings_table.sql",
                "db/migration/V9__add_commit_sha_to_code_reviews.sql"
        );

        for (String migration : expectedMigrations) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(migration);
            assertThat(is).as("Flyway migration resource must exist: " + migration).isNotNull();
        }
    }

    @Test
    void testApplicationYamlHasNoCommittedSecrets() throws Exception {
        Path appYamlPath = findFilePath("backend/src/main/resources/application.yml");
        if (appYamlPath == null || !Files.exists(appYamlPath)) {
            appYamlPath = findFilePath("src/main/resources/application.yml");
        }
        assertThat(appYamlPath).isNotNull().exists();

        String content = Files.readString(appYamlPath);

        // Ensure real secret placeholders are used instead of hardcoded values
        assertThat(content).contains("${GITHUB_APP_ID:}");
        assertThat(content).contains("${GITHUB_PRIVATE_KEY:}");
        assertThat(content).contains("${GEMINI_API_KEY:}");
        assertThat(content).contains("${GITHUB_WEBHOOK_SECRET:}");
    }

    @Test
    void testProductionStartupValidationFailsOnDevSecret() {
        JwtProperties jwtProperties = new JwtProperties("defaultDevSecretKeyThatIsAtLeast32BytesLongForHMACSHA256Signatures12345", 3600000L, "Bearer", "ai-code-review-bot");
        GithubProperties githubProperties = new GithubProperties();
        githubProperties.setAppId("123456");
        githubProperties.setPrivateKey("-----BEGIN RSA PRIVATE KEY-----\ndummyKey\n-----END RSA PRIVATE KEY-----");
        GeminiProperties geminiProperties = new GeminiProperties("realApiKey123", "gemini-2.5-flash", "https://generativelanguage.googleapis.com");

        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.password", "prodPassword");

        ProductionConfigValidator validator = new ProductionConfigValidator(jwtProperties, githubProperties, geminiProperties, env);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be configured with a secure production secret");
    }

    private Path findFilePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parent = Paths.get("..", relativePath);
        if (Files.exists(parent)) {
            return parent;
        }
        Path userDir = Paths.get(System.getProperty("user.dir"), relativePath);
        if (Files.exists(userDir)) {
            return userDir;
        }
        Path parentUserDir = Paths.get(System.getProperty("user.dir"), "..", relativePath);
        if (Files.exists(parentUserDir)) {
            return parentUserDir;
        }
        return null;
    }
}
