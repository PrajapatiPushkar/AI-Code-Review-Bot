package com.pushkar.codereview.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret = "defaultDevSecretKeyThatIsAtLeast32BytesLongForHMACSHA256Signatures12345";

    private long expirationMs = 3600000L; // 1 hour

    private String tokenType = "Bearer";

    private String issuer = "ai-code-review-bot";

    public JwtProperties() {
    }

    public JwtProperties(String secret, long expirationMs, String tokenType, String issuer) {
        if (secret != null && !secret.isBlank()) {
            this.secret = secret;
        }
        this.expirationMs = expirationMs;
        if (tokenType != null && !tokenType.isBlank()) {
            this.tokenType = tokenType;
        }
        if (issuer != null && !issuer.isBlank()) {
            this.issuer = issuer;
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String toString() {
        return "JwtProperties{" +
                "secret='" + (secret != null && !secret.isBlank() ? "[PROTECTED]" : null) + '\'' +
                ", expirationMs=" + expirationMs +
                ", tokenType='" + tokenType + '\'' +
                ", issuer='" + issuer + '\'' +
                '}';
    }
}
