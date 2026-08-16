package com.pushkar.codereview.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "github")
@Validated
public class GithubProperties {

    private String appId;
    private String appName;
    private String privateKey;
    private String privateKeyPath;
    private String webhookSecret;

    @NotBlank(message = "GitHub API base URL must not be blank")
    private String apiBaseUrl = "https://api.github.com";

    public GithubProperties() {
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public String toString() {
        return "GithubProperties{" +
                "appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", privateKey='" + (privateKey != null && !privateKey.isBlank() ? "[PROTECTED]" : null) + '\'' +
                ", privateKeyPath='" + privateKeyPath + '\'' +
                ", webhookSecret='" + (webhookSecret != null && !webhookSecret.isBlank() ? "[PROTECTED]" : null) + '\'' +
                ", apiBaseUrl='" + apiBaseUrl + '\'' +
                '}';
    }
}
