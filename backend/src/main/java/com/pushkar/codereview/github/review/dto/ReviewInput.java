package com.pushkar.codereview.github.review.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReviewInput {

    private Long repositoryId;
    private String repositoryName;
    private String repositoryFullName;
    private String repositoryUrl;
    private String defaultBranch;

    private Long pullRequestId;
    private Long pullRequestNumber;
    private String title;
    private String body;
    private String state;
    private String pullRequestUrl;
    private String authorLogin;
    private String headBranch;
    private String baseBranch;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ReviewFileInput> files;

    public ReviewInput() {
        this.files = new ArrayList<>();
    }

    public ReviewInput(Long repositoryId,
                       String repositoryName,
                       String repositoryFullName,
                       String repositoryUrl,
                       String defaultBranch,
                       Long pullRequestId,
                       Long pullRequestNumber,
                       String title,
                       String body,
                       String state,
                       String pullRequestUrl,
                       String authorLogin,
                       String headBranch,
                       String baseBranch,
                       Instant createdAt,
                       Instant updatedAt,
                       List<ReviewFileInput> files) {
        this.repositoryId = repositoryId;
        this.repositoryName = repositoryName;
        this.repositoryFullName = repositoryFullName;
        this.repositoryUrl = repositoryUrl;
        this.defaultBranch = defaultBranch;
        this.pullRequestId = pullRequestId;
        this.pullRequestNumber = pullRequestNumber;
        this.title = title;
        this.body = body;
        this.state = state;
        this.pullRequestUrl = pullRequestUrl;
        this.authorLogin = authorLogin;
        this.headBranch = headBranch;
        this.baseBranch = baseBranch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.files = files != null ? files : Collections.emptyList();
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public Long getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(Long pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Long pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public void setPullRequestUrl(String pullRequestUrl) {
        this.pullRequestUrl = pullRequestUrl;
    }

    public String getAuthorLogin() {
        return authorLogin;
    }

    public void setAuthorLogin(String authorLogin) {
        this.authorLogin = authorLogin;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public void setHeadBranch(String headBranch) {
        this.headBranch = headBranch;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
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

    public List<ReviewFileInput> getFiles() {
        return files != null ? files : Collections.emptyList();
    }

    public void setFiles(List<ReviewFileInput> files) {
        this.files = files != null ? files : Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewInput that = (ReviewInput) o;
        return Objects.equals(repositoryId, that.repositoryId) &&
                Objects.equals(repositoryName, that.repositoryName) &&
                Objects.equals(repositoryFullName, that.repositoryFullName) &&
                Objects.equals(repositoryUrl, that.repositoryUrl) &&
                Objects.equals(defaultBranch, that.defaultBranch) &&
                Objects.equals(pullRequestId, that.pullRequestId) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(title, that.title) &&
                Objects.equals(body, that.body) &&
                Objects.equals(state, that.state) &&
                Objects.equals(pullRequestUrl, that.pullRequestUrl) &&
                Objects.equals(authorLogin, that.authorLogin) &&
                Objects.equals(headBranch, that.headBranch) &&
                Objects.equals(baseBranch, that.baseBranch) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, repositoryName, repositoryFullName, repositoryUrl, defaultBranch,
                pullRequestId, pullRequestNumber, title, body, state, pullRequestUrl,
                authorLogin, headBranch, baseBranch, createdAt, updatedAt, files);
    }

    @Override
    public String toString() {
        return "ReviewInput{" +
                "repositoryId=" + repositoryId +
                ", repositoryName='" + repositoryName + '\'' +
                ", repositoryFullName='" + repositoryFullName + '\'' +
                ", repositoryUrl='" + repositoryUrl + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                ", pullRequestId=" + pullRequestId +
                ", pullRequestNumber=" + pullRequestNumber +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", state='" + state + '\'' +
                ", pullRequestUrl='" + pullRequestUrl + '\'' +
                ", authorLogin='" + authorLogin + '\'' +
                ", headBranch='" + headBranch + '\'' +
                ", baseBranch='" + baseBranch + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", files=" + files +
                '}';
    }
}
