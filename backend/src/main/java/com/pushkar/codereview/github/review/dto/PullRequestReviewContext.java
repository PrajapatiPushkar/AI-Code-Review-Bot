package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PullRequestReviewContext {

    private GithubRepositoryResponse repository;
    private GithubPullRequestResponse pullRequest;
    private List<GithubPullRequestFileResponse> changedFiles;

    public PullRequestReviewContext() {
        this.changedFiles = new ArrayList<>();
    }

    public PullRequestReviewContext(GithubRepositoryResponse repository,
                                    GithubPullRequestResponse pullRequest,
                                    List<GithubPullRequestFileResponse> changedFiles) {
        this.repository = repository;
        this.pullRequest = pullRequest;
        this.changedFiles = changedFiles != null ? changedFiles : Collections.emptyList();
    }

    public GithubRepositoryResponse getRepository() {
        return repository;
    }

    public void setRepository(GithubRepositoryResponse repository) {
        this.repository = repository;
    }

    public GithubPullRequestResponse getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(GithubPullRequestResponse pullRequest) {
        this.pullRequest = pullRequest;
    }

    public List<GithubPullRequestFileResponse> getChangedFiles() {
        return changedFiles != null ? changedFiles : Collections.emptyList();
    }

    public void setChangedFiles(List<GithubPullRequestFileResponse> changedFiles) {
        this.changedFiles = changedFiles != null ? changedFiles : Collections.emptyList();
    }
}
