package com.pushkar.codereview.github.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class CodeReviewExecutionResult {

    private Long codeReviewId;
    private Long installationId;
    private String owner;
    private String repository;
    private Long pullRequestNumber;
    private String status;
    private String reviewSummary;
    private int totalFindings;
    private int postedCommentsCount;

    @JsonProperty("created")
    private boolean created = true;

    @JsonProperty("commitSha")
    private String commitSha;

    public CodeReviewExecutionResult() {
    }

    public CodeReviewExecutionResult(String repository, Long pullRequestNumber, String reviewSummary, int totalFindings, int postedCommentsCount) {
        this(null, null, null, repository, pullRequestNumber, "COMPLETED", reviewSummary, totalFindings, postedCommentsCount, true, null);
    }

    public CodeReviewExecutionResult(Long codeReviewId, Long installationId, String owner, String repository, Long pullRequestNumber, String status, String reviewSummary, int totalFindings, int postedCommentsCount) {
        this(codeReviewId, installationId, owner, repository, pullRequestNumber, status, reviewSummary, totalFindings, postedCommentsCount, true, null);
    }

    public CodeReviewExecutionResult(Long codeReviewId, Long installationId, String owner, String repository, Long pullRequestNumber, String status, String reviewSummary, int totalFindings, int postedCommentsCount, boolean created) {
        this(codeReviewId, installationId, owner, repository, pullRequestNumber, status, reviewSummary, totalFindings, postedCommentsCount, created, null);
    }

    public CodeReviewExecutionResult(Long codeReviewId, Long installationId, String owner, String repository, Long pullRequestNumber, String status, String reviewSummary, int totalFindings, int postedCommentsCount, boolean created, String commitSha) {
        this.codeReviewId = codeReviewId;
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.status = status;
        this.reviewSummary = reviewSummary;
        this.totalFindings = totalFindings;
        this.postedCommentsCount = postedCommentsCount;
        this.created = created;
        this.commitSha = commitSha;
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

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(int totalFindings) {
        this.totalFindings = totalFindings;
    }

    public int getPostedCommentsCount() {
        return postedCommentsCount;
    }

    public void setPostedCommentsCount(int postedCommentsCount) {
        this.postedCommentsCount = postedCommentsCount;
    }

    public boolean isCreated() {
        return created;
    }

    public boolean getCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
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
        CodeReviewExecutionResult that = (CodeReviewExecutionResult) o;
        return totalFindings == that.totalFindings &&
                postedCommentsCount == that.postedCommentsCount &&
                created == that.created &&
                Objects.equals(codeReviewId, that.codeReviewId) &&
                Objects.equals(installationId, that.installationId) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(status, that.status) &&
                Objects.equals(reviewSummary, that.reviewSummary) &&
                Objects.equals(commitSha, that.commitSha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeReviewId, installationId, owner, repository, pullRequestNumber, status, reviewSummary, totalFindings, postedCommentsCount, created, commitSha);
    }

    @Override
    public String toString() {
        return "CodeReviewExecutionResult{" +
                "codeReviewId=" + codeReviewId +
                ", installationId=" + installationId +
                ", owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", status='" + status + '\'' +
                ", reviewSummary='" + reviewSummary + '\'' +
                ", totalFindings=" + totalFindings +
                ", postedCommentsCount=" + postedCommentsCount +
                ", created=" + created +
                ", commitSha='" + commitSha + '\'' +
                '}';
    }
}
