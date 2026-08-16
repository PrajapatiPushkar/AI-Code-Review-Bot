package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GithubInstallationRepositoriesResponse {

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("repositories")
    private List<GithubRepositoryResponse> repositories;

    public GithubInstallationRepositoriesResponse() {
    }

    public GithubInstallationRepositoriesResponse(int totalCount, List<GithubRepositoryResponse> repositories) {
        this.totalCount = totalCount;
        this.repositories = repositories;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<GithubRepositoryResponse> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<GithubRepositoryResponse> repositories) {
        this.repositories = repositories;
    }
}
