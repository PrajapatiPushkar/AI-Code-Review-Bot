package com.pushkar.codereview.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class GithubInstallationCreateRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    private Long userId;

    @NotNull(message = "GitHub installation ID is required")
    @Positive(message = "GitHub installation ID must be a positive number")
    private Long githubInstallationId;

    @NotBlank(message = "GitHub account login is required")
    @Size(max = 255, message = "GitHub account login must not exceed 255 characters")
    private String githubAccountLogin;

    @NotBlank(message = "GitHub account type is required")
    @Size(max = 50, message = "GitHub account type must not exceed 50 characters")
    private String githubAccountType;

    public GithubInstallationCreateRequest() {
    }

    public GithubInstallationCreateRequest(Long userId, Long githubInstallationId, String githubAccountLogin, String githubAccountType) {
        this.userId = userId;
        this.githubInstallationId = githubInstallationId;
        this.githubAccountLogin = githubAccountLogin;
        this.githubAccountType = githubAccountType;
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
}
