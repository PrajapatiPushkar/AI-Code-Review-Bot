package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class GithubReviewCommentRequest {

    @JsonProperty("body")
    private String body;

    @JsonProperty("commit_id")
    private String commitId;

    @JsonProperty("path")
    private String path;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("side")
    private String side = "RIGHT";

    public GithubReviewCommentRequest() {
    }

    public GithubReviewCommentRequest(String body, String commitId, String path, Integer line, String side) {
        this.body = body;
        this.commitId = commitId;
        this.path = path;
        this.line = line;
        this.side = side != null ? side : "RIGHT";
    }

    public GithubReviewCommentRequest(String body, String commitId, String path, Integer line) {
        this(body, commitId, path, line, "RIGHT");
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubReviewCommentRequest that = (GithubReviewCommentRequest) o;
        return Objects.equals(body, that.body) &&
                Objects.equals(commitId, that.commitId) &&
                Objects.equals(path, that.path) &&
                Objects.equals(line, that.line) &&
                Objects.equals(side, that.side);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, commitId, path, line, side);
    }

    @Override
    public String toString() {
        return "GithubReviewCommentRequest{" +
                "body='" + body + '\'' +
                ", commitId='" + commitId + '\'' +
                ", path='" + path + '\'' +
                ", line=" + line +
                ", side='" + side + '\'' +
                '}';
    }
}
