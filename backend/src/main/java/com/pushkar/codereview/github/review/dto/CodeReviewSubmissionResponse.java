package com.pushkar.codereview.github.review.dto;

import java.util.Objects;

public class CodeReviewSubmissionResponse {

    private Long codeReviewId;
    private Long installationId;
    private String owner;
    private String repository;
    private Long pullRequestNumber;
    private String status;

    public CodeReviewSubmissionResponse() {
    }

    public CodeReviewSubmissionResponse(Long codeReviewId, Long installationId, String owner, String repository, Long pullRequestNumber, String status) {
        this.codeReviewId = codeReviewId;
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.status = status;
    }

    public Long getCodeReviewId() {
        return codeReviewId;
    }

    public void setCodeReviewId(Long codeReviewId) {
        this.codeReviewId = codeReviewId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewSubmissionResponse that = (CodeReviewSubmissionResponse) o;
        return Objects.equals(codeReviewId, that.codeReviewId) &&
                Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeReviewId, installationId, owner, repository, pullRequestNumber, status);
    }

    @Override
    public String toString() {
        return "CodeReviewSubmissionResponse{" +
                "codeReviewId=" + codeReviewId +
                ", installationId=" + installationId +
                ", owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", status='" + status + '\'' +
                '}';
    }
}
