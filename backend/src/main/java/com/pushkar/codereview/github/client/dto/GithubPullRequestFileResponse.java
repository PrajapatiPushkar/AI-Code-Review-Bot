package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubPullRequestFileResponse {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("status")
    private String status;

    @JsonProperty("additions")
    private Integer additions;

    @JsonProperty("deletions")
    private Integer deletions;

    @JsonProperty("changes")
    private Integer changes;

    @JsonProperty("patch")
    private String patch;

    @JsonProperty("previous_filename")
    private String previousFilename;

    public GithubPullRequestFileResponse() {
    }

    public GithubPullRequestFileResponse(String sha, String filename, String status, Integer additions,
                                        Integer deletions, Integer changes, String patch, String previousFilename) {
        this.sha = sha;
        this.filename = filename;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.patch = patch;
        this.previousFilename = previousFilename;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAdditions() {
        return additions;
    }

    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public Integer getChanges() {
        return changes;
    }

    public void setChanges(Integer changes) {
        this.changes = changes;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    public String getPreviousFilename() {
        return previousFilename;
    }

    public void setPreviousFilename(String previousFilename) {
        this.previousFilename = previousFilename;
    }
}
