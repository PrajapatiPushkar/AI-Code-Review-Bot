package com.pushkar.codereview.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Validated
public class GeminiProperties {

    private String apiKey;

    @NotBlank(message = "Gemini model must not be blank")
    private String model = "gemini-3.6-flash";

    @NotBlank(message = "Gemini API base URL must not be blank")
    private String apiBaseUrl = "https://generativelanguage.googleapis.com";

    public GeminiProperties() {
    }

    public GeminiProperties(String apiKey, String model, String apiBaseUrl) {
        this.apiKey = apiKey;
        if (model != null) {
            this.model = model;
        }
        if (apiBaseUrl != null) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public String toString() {
        return "GeminiProperties{" +
                "apiKey='" + (apiKey != null && !apiKey.isBlank() ? "[PROTECTED]" : null) + '\'' +
                ", model='" + model + '\'' +
                ", apiBaseUrl='" + apiBaseUrl + '\'' +
                '}';
    }
}
