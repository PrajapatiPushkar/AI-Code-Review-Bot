package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubInstallationRepositoriesResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubRepositoryClient {

    private final RestClient restClient;
    private final GithubInstallationTokenService tokenService;

    @org.springframework.beans.factory.annotation.Autowired
    public GithubRepositoryClient(GithubProperties githubProperties,
                                  GithubInstallationTokenService tokenService,
                                  RestClient.Builder restClientBuilder) {
        this.tokenService = tokenService;
        this.restClient = restClientBuilder
                .baseUrl(githubProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public GithubRepositoryClient(RestClient restClient, GithubInstallationTokenService tokenService) {
        this.restClient = restClient;
        this.tokenService = tokenService;
    }

    public GithubInstallationRepositoriesResponse getInstallationRepositories(Long installationId, int page, int perPage) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        if (perPage < 1 || perPage > 100) {
            throw new IllegalArgumentException("Per-page limit must be between 1 and 100");
        }

        String token = tokenService.getInstallationAccessToken(installationId);
        if (token == null || token.isBlank()) {
            throw new GithubApiException("Failed to obtain installation access token for installation ID: " + installationId, 401);
        }

        try {
            return restClient.get()
                    .uri("/installation/repositories?page={page}&per_page={perPage}", page, perPage)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            throw new GithubApiException("GitHub API authentication/authorization failed (HTTP " + status + ")", status);
                        } else if (status == 404) {
                            throw new ResourceNotFoundException("GitHub installation not found on GitHub with ID: " + installationId);
                        } else if (status >= 500 && status < 600) {
                            throw new GithubApiException("GitHub API server error (HTTP " + status + ")", 502);
                        } else {
                            throw new GithubApiException("GitHub API request failed with status HTTP " + status, status);
                        }
                    })
                    .body(GithubInstallationRepositoriesResponse.class);
        } catch (GithubApiException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubApiException("Failed to communicate with GitHub API", 502, e);
        }
    }

    public GithubRepositoryResponse getRepository(Long installationId, String owner, String repository) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }

        String token = tokenService.getInstallationAccessToken(installationId);
        if (token == null || token.isBlank()) {
            throw new GithubApiException("Failed to obtain installation access token for installation ID: " + installationId, 401);
        }

        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repository)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            throw new GithubApiException("GitHub API authentication/authorization failed (HTTP " + status + ")", status);
                        } else if (status == 404) {
                            throw new ResourceNotFoundException("GitHub repository not found: " + owner + "/" + repository);
                        } else if (status >= 500 && status < 600) {
                            throw new GithubApiException("GitHub API server error (HTTP " + status + ")", 502);
                        } else {
                            throw new GithubApiException("GitHub API request failed with status HTTP " + status, status);
                        }
                    })
                    .body(GithubRepositoryResponse.class);
        } catch (GithubApiException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubApiException("Failed to communicate with GitHub API", 502, e);
        }
    }

    public GithubRepositoryResponse getRepository(String owner, String repository, Long installationId) {
        return getRepository(installationId, owner, repository);
    }
}
