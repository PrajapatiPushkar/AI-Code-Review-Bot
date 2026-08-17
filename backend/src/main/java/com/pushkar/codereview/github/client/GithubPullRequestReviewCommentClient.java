package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentRequest;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubPullRequestReviewCommentClient {

    private final RestClient restClient;
    private final GithubInstallationTokenService tokenService;

    @org.springframework.beans.factory.annotation.Autowired
    public GithubPullRequestReviewCommentClient(GithubProperties githubProperties,
                                               GithubInstallationTokenService tokenService,
                                               RestClient.Builder restClientBuilder) {
        this.tokenService = tokenService;
        this.restClient = restClientBuilder
                .baseUrl(githubProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public GithubPullRequestReviewCommentClient(RestClient restClient, GithubInstallationTokenService tokenService) {
        this.restClient = restClient;
        this.tokenService = tokenService;
    }

    public GithubReviewCommentResponse createReviewComment(Long installationId,
                                                            String owner,
                                                            String repository,
                                                            long pullRequestNumber,
                                                            GithubReviewCommentRequest request) {
        validateInputs(installationId, owner, repository, pullRequestNumber, request);

        String token = tokenService.getInstallationAccessToken(installationId);
        if (token == null || token.isBlank()) {
            throw new GithubApiException("Failed to obtain installation access token for installation ID: " + installationId, 401);
        }

        try {
            return restClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{pull_number}/comments", owner, repository, pullRequestNumber)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        int status = resp.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            throw new GithubApiException("GitHub API authentication/authorization failed (HTTP " + status + ")", status);
                        } else if (status == 404) {
                            throw new ResourceNotFoundException("GitHub pull request not found: " + owner + "/" + repository + "#" + pullRequestNumber);
                        } else if (status == 422) {
                            throw new GithubApiException("GitHub API comment validation failed (HTTP 422)", 422);
                        } else if (status >= 500 && status < 600) {
                            throw new GithubApiException("GitHub API server error (HTTP " + status + ")", 502);
                        } else {
                            throw new GithubApiException("GitHub API request failed with status HTTP " + status, status);
                        }
                    })
                    .body(GithubReviewCommentResponse.class);
        } catch (GithubApiException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubApiException("Failed to communicate with GitHub API", 502, e);
        }
    }

    private void validateInputs(Long installationId, String owner, String repository, long pullRequestNumber, GithubReviewCommentRequest request) {
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
        if (request == null) {
            throw new IllegalArgumentException("GithubReviewCommentRequest must not be null");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        if (request.getCommitId() == null || request.getCommitId().isBlank()) {
            throw new IllegalArgumentException("Comment commit_id must not be blank");
        }
        if (request.getPath() == null || request.getPath().isBlank()) {
            throw new IllegalArgumentException("Comment path must not be blank");
        }
        if (request.getLine() == null || request.getLine() <= 0) {
            throw new IllegalArgumentException("Comment line must be a positive integer");
        }
    }
}
