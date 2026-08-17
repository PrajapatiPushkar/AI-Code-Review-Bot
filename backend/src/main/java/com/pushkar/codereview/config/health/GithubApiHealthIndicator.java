package com.pushkar.codereview.config.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GithubApiHealthIndicator implements HealthIndicator {

    private final String appId;
    private final String privateKey;

    @Autowired
    public GithubApiHealthIndicator(@Value("${github.app.id:}") String appId,
                                    @Value("${github.app.private-key:}") String privateKey) {
        this.appId = appId;
        this.privateKey = privateKey;
    }

    @Override
    public Health health() {
        boolean hasAppId = appId != null && !appId.isBlank() && !appId.equals("0");
        boolean hasPrivateKey = privateKey != null && !privateKey.isBlank() && !privateKey.contains("PLACEHOLDER");

        if (hasAppId && hasPrivateKey) {
            return Health.up()
                    .withDetail("service", "GitHub API Integration")
                    .withDetail("status", "CONFIGURED")
                    .build();
        } else {
            return Health.down()
                    .withDetail("service", "GitHub API Integration")
                    .withDetail("status", "NOT_CONFIGURED")
                    .withDetail("reason", "GitHub App credentials missing or invalid")
                    .build();
        }
    }
}
