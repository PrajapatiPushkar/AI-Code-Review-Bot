package com.pushkar.codereview.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProductionConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfigValidator.class);

    private final JwtProperties jwtProperties;
    private final GithubProperties githubProperties;
    private final GeminiProperties geminiProperties;
    private final Environment environment;

    public ProductionConfigValidator(JwtProperties jwtProperties,
                                     GithubProperties githubProperties,
                                     GeminiProperties geminiProperties,
                                     Environment environment) {
        this.jwtProperties = jwtProperties;
        this.githubProperties = githubProperties;
        this.geminiProperties = geminiProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        String jwtSecret = jwtProperties.getSecret();
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("defaultDevSecretKey")) {
            errors.add("JWT_SECRET must be configured with a secure production secret");
        }

        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword == null || dbPassword.isBlank()) {
            errors.add("Database password (DB_PASSWORD / SPRING_DATASOURCE_PASSWORD) must not be blank in production");
        }

        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            errors.add("GEMINI_API_KEY must not be blank in production");
        }

        if (githubProperties.getAppId() == null || githubProperties.getAppId().isBlank()) {
            errors.add("GITHUB_APP_ID must not be blank in production");
        }

        boolean hasKey = githubProperties.getPrivateKey() != null && !githubProperties.getPrivateKey().isBlank();
        boolean hasKeyPath = githubProperties.getPrivateKeyPath() != null && !githubProperties.getPrivateKeyPath().isBlank();
        if (!hasKey && !hasKeyPath) {
            errors.add("GITHUB_PRIVATE_KEY or GITHUB_PRIVATE_KEY_PATH must be configured in production");
        }

        if (!errors.isEmpty()) {
            log.error("Production startup validation failed with {} error(s):", errors.size());
            for (String err : errors) {
                log.error(" - {}", err);
            }
            throw new IllegalStateException("Production startup failed due to missing required configuration: " + String.join("; ", errors));
        }

        log.info("Production configuration validation passed successfully");
    }
}
