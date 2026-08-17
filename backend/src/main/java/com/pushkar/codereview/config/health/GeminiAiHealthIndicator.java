package com.pushkar.codereview.config.health;

import com.pushkar.codereview.github.review.ai.AiReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiHealthIndicator implements HealthIndicator {

    private final AiReviewService aiReviewService;
    private final String apiKey;

    @Autowired
    public GeminiAiHealthIndicator(@Autowired(required = false) AiReviewService aiReviewService,
                                   @Value("${gemini.api.key:}") String apiKey) {
        this.aiReviewService = aiReviewService;
        this.apiKey = apiKey;
    }

    @Override
    public Health health() {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("PLACEHOLDER")) {
            return Health.up()
                    .withDetail("service", "Gemini AI Engine")
                    .withDetail("status", "CONFIGURED")
                    .build();
        } else {
            return Health.down()
                    .withDetail("service", "Gemini AI Engine")
                    .withDetail("status", "NOT_CONFIGURED")
                    .withDetail("reason", "Gemini API key is missing or set to placeholder")
                    .build();
        }
    }
}
