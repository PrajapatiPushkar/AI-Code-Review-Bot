package com.pushkar.codereview.github.dto;

import java.time.Instant;

public class GithubInstallationResponse {

    private Long id;
    private Long userId;
    private Long githubInstallationId;
    private String githubAccountLogin;
    private String githubAccountType;
    private boolean verified;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public GithubInstallationResponse() {
    }

    public GithubInstallationResponse(Long id, Long userId, Long githubInstallationId, String githubAccountLogin, String githubAccountType, Instant createdAt, Instant updatedAt) {
        this(id, userId, githubInstallationId, githubAccountLogin, githubAccountType, false, null, createdAt, updatedAt);
    }

    public GithubInstallationResponse(Long id, Long userId, Long githubInstallationId, String githubAccountLogin, String githubAccountType, boolean verified, Instant verifiedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.githubInstallationId = githubInstallationId;
        this.githubAccountLogin = githubAccountLogin;
        this.githubAccountType = githubAccountType;
        this.verified = verified;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGithubInstallationId() {
        return githubInstallationId;
    }

    public void setGithubInstallationId(Long githubInstallationId) {
        this.githubInstallationId = githubInstallationId;
    }

    public String getGithubAccountLogin() {
        return githubAccountLogin;
    }

    public void setGithubAccountLogin(String githubAccountLogin) {
        this.githubAccountLogin = githubAccountLogin;
    }

    public String getGithubAccountType() {
        return githubAccountType;
    }

    public void setGithubAccountType(String githubAccountType) {
        this.githubAccountType = githubAccountType;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
