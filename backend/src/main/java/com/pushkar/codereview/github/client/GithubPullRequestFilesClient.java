package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class GithubPullRequestFilesClient {

    private static final int DEFAULT_PER_PAGE = 100;
    private static final int MAX_PAGES = 30;

    private final RestClient restClient;
    private final GithubInstallationTokenService tokenService;

    public GithubPullRequestFilesClient(GithubProperties githubProperties,
                                        GithubInstallationTokenService tokenService,
                                        RestClient.Builder restClientBuilder) {
        this.tokenService = tokenService;
        this.restClient = restClientBuilder
                .baseUrl(githubProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public GithubPullRequestFilesClient(RestClient restClient, GithubInstallationTokenService tokenService) {
        this.restClient = restClient;
        this.tokenService = tokenService;
    }

    public List<GithubPullRequestFileResponse> getChangedFiles(Long installationId, String owner, String repository, long pullRequestNumber) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
        if (pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be a positive number");
        }

        String token = tokenService.getInstallationAccessToken(installationId);
        if (token == null || token.isBlank()) {
            throw new GithubApiException("Failed to obtain installation access token for installation ID: " + installationId, 401);
        }

        List<GithubPullRequestFileResponse> allFiles = new ArrayList<>();
        int page = 1;
        int perPage = DEFAULT_PER_PAGE;

        try {
            while (page <= MAX_PAGES) {
                int currentPage = page;
                GithubPullRequestFileResponse[] filesPage = restClient.get()
                        .uri("/repos/{owner}/{repo}/pulls/{pull_number}/files?page={page}&per_page={per_page}",
                                owner, repository, pullRequestNumber, currentPage, perPage)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (request, response) -> {
                            int status = response.getStatusCode().value();
                            if (status == 401 || status == 403) {
                                throw new GithubApiException("GitHub API authentication/authorization failed (HTTP " + status + ")", status);
                            } else if (status == 404) {
                                throw new ResourceNotFoundException("GitHub pull request not found: " + owner + "/" + repository + "#" + pullRequestNumber);
                            } else if (status >= 500 && status < 600) {
                                throw new GithubApiException("GitHub API server error (HTTP " + status + ")", 502);
                            } else {
                                throw new GithubApiException("GitHub API request failed with status HTTP " + status, status);
                            }
                        })
                        .body(GithubPullRequestFileResponse[].class);

                if (filesPage == null || filesPage.length == 0) {
                    break;
                }

                allFiles.addAll(Arrays.asList(filesPage));

                if (filesPage.length < perPage) {
                    break;
                }

                if (page == MAX_PAGES) {
                    throw new GithubApiException("Exceeded maximum page limit (" + MAX_PAGES + ") for changed files on PR: " + owner + "/" + repository + "#" + pullRequestNumber, 400);
                }

                page++;
            }
        } catch (GithubApiException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubApiException("Failed to communicate with GitHub API", 502, e);
        }

        return allFiles;
    }

    public List<GithubPullRequestFileResponse> getChangedFiles(String owner, String repository, long pullRequestNumber, Long installationId) {
        return getChangedFiles(installationId, owner, repository, pullRequestNumber);
    }
}
