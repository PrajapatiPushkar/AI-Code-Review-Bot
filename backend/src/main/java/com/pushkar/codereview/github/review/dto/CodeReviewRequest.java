package com.pushkar.codereview.github.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

public class CodeReviewRequest {

    @NotNull(message = "Installation ID must not be null")
    @Positive(message = "Installation ID must be a positive number")
    private Long installationId;

    @NotBlank(message = "Repository owner must not be blank")
    private String owner;

    @NotBlank(message = "Repository name must not be blank")
    private String repository;

    @NotNull(message = "Pull request number must not be null")
    @Positive(message = "Pull request number must be a positive number")
    private Integer pullRequestNumber;

    public CodeReviewRequest() {
    }

    public CodeReviewRequest(Long installationId, String owner, String repository, Integer pullRequestNumber) {
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Integer pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewRequest that = (CodeReviewRequest) o;
        return Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installationId, owner, repository, pullRequestNumber);
    }

    @Override
    public String toString() {
        return "CodeReviewRequest{" +
                "installationId=" + installationId +
                ", owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                '}';
    }
}
