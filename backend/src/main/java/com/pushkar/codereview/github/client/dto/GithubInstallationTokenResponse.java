package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class GithubInstallationTokenResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    public GithubInstallationTokenResponse() {
    }

    public GithubInstallationTokenResponse(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
