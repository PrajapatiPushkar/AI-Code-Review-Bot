package com.pushkar.codereview.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class GithubInstallationRequest {

    @NotNull(message = "Installation ID is required")
    @Positive(message = "Installation ID must be a positive number")
    @JsonAlias({"githubInstallationId", "installation_id"})
    private Long installationId;

    @NotBlank(message = "GitHub account login is required")
    @Size(max = 255, message = "GitHub account login must not exceed 255 characters")
    @JsonAlias({"accountLogin", "account_login"})
    private String githubAccountLogin;

    @Size(max = 50, message = "GitHub account type must not exceed 50 characters")
    @JsonAlias({"accountType", "account_type"})
    private String githubAccountType;

    public GithubInstallationRequest() {
    }

    public GithubInstallationRequest(Long installationId, String githubAccountLogin, String githubAccountType) {
        this.installationId = installationId;
        this.githubAccountLogin = githubAccountLogin;
        this.githubAccountType = githubAccountType;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
    }

    public String getGithubAccountLogin() {
        return githubAccountLogin;
    }

    public void setGithubAccountLogin(String githubAccountLogin) {
        this.githubAccountLogin = githubAccountLogin;
    }

    public String getGithubAccountType() {
        return (githubAccountType != null && !githubAccountType.isBlank()) ? githubAccountType : "User";
    }

    public void setGithubAccountType(String githubAccountType) {
        this.githubAccountType = githubAccountType;
    }
}
