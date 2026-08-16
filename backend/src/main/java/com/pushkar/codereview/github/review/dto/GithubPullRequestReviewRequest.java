package com.pushkar.codereview.github.review.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

public class GithubPullRequestReviewRequest {

    @NotNull(message = "Installation ID must not be null")
    @Positive(message = "Installation ID must be a positive number")
    @JsonAlias({"githubInstallationId", "installation_id"})
    private Long installationId;

    @NotBlank(message = "Repository owner must not be blank")
    private String owner;

    @NotBlank(message = "Repository name must not be blank")
    private String repository;

    @NotNull(message = "Pull request number must not be null")
    @Positive(message = "Pull request number must be a positive number")
    @JsonAlias({"pull_request_number", "pullRequestNumber", "prNumber"})
    private Long pullRequestNumber;

    @JsonAlias({"commitSha", "commit_sha", "commit", "sha"})
    private String commitSha;

    public GithubPullRequestReviewRequest() {
    }

    public GithubPullRequestReviewRequest(Long installationId, String owner, String repository, Long pullRequestNumber) {
        this(installationId, owner, repository, pullRequestNumber, null);
    }

    public GithubPullRequestReviewRequest(Long installationId, String owner, String repository, Long pullRequestNumber, String commitSha) {
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
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

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Long pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubPullRequestReviewRequest that = (GithubPullRequestReviewRequest) o;
        return Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installationId, owner, repository, pullRequestNumber);
    }
}
