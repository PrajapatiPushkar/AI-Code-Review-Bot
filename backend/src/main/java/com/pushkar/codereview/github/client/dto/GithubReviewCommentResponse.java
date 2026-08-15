package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubReviewCommentResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("body")
    private String body;

    @JsonProperty("path")
    private String path;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("commit_id")
    private String commitId;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("created_at")
    private Instant createdAt;

    public GithubReviewCommentResponse() {
    }

    public GithubReviewCommentResponse(Long id, String body, String path, Integer line, String commitId, String htmlUrl, Instant createdAt) {
        this.id = id;
        this.body = body;
        this.path = path;
        this.line = line;
        this.commitId = commitId;
        this.htmlUrl = htmlUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubReviewCommentResponse that = (GithubReviewCommentResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(body, that.body) &&
                Objects.equals(path, that.path) &&
                Objects.equals(line, that.line) &&
                Objects.equals(commitId, that.commitId) &&
                Objects.equals(htmlUrl, that.htmlUrl) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, body, path, line, commitId, htmlUrl, createdAt);
    }

    @Override
    public String toString() {
        return "GithubReviewCommentResponse{" +
                "id=" + id +
                ", body='" + body + '\'' +
                ", path='" + path + '\'' +
                ", line=" + line +
                ", commitId='" + commitId + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
