package com.pushkar.codereview.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class RepositoryCreateRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    private Long userId;

    @NotNull(message = "GitHub repository ID is required")
    @Positive(message = "GitHub repository ID must be a positive number")
    private Long githubRepositoryId;

    @NotBlank(message = "Repository name is required")
    @Size(max = 255, message = "Repository name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Full name is required")
    @Size(max = 500, message = "Full name must not exceed 500 characters")
    private String fullName;

    @NotBlank(message = "Default branch is required")
    @Size(max = 255, message = "Default branch must not exceed 255 characters")
    private String defaultBranch;

    @NotBlank(message = "HTML URL is required")
    @Size(max = 1000, message = "HTML URL must not exceed 1000 characters")
    private String htmlUrl;

    private Boolean isActive = true;

    public RepositoryCreateRequest() {
    }

    public RepositoryCreateRequest(Long userId, Long githubRepositoryId, String name, String fullName, String defaultBranch, String htmlUrl, Boolean isActive) {
        this.userId = userId;
        this.githubRepositoryId = githubRepositoryId;
        this.name = name;
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public void setGithubRepositoryId(Long githubRepositoryId) {
        this.githubRepositoryId = githubRepositoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
