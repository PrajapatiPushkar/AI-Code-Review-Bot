package com.pushkar.codereview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false",
        "github.app.id=123456",
        "github.app.private-key=test-key-content",
        "gemini.api.key=test-gemini-api-key"
})
@ActiveProfiles("test")
class AiCodeReviewBotApplicationTests {

    @Test
    void contextLoads() {
    }
}
