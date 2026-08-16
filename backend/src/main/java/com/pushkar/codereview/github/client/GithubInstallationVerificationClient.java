package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubJwtService;
import com.pushkar.codereview.github.client.dto.GithubInstallationDetailsResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubInstallationVerificationClient {

    private final RestClient restClient;
    private final GithubJwtService githubJwtService;

    public GithubInstallationVerificationClient(GithubProperties githubProperties,
                                                 GithubJwtService githubJwtService,
                                                 RestClient.Builder restClientBuilder) {
        this.githubJwtService = githubJwtService;
        this.restClient = restClientBuilder
                .baseUrl(githubProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public GithubInstallationVerificationClient(RestClient restClient, GithubJwtService githubJwtService) {
        this.restClient = restClient;
        this.githubJwtService = githubJwtService;
    }

    public GithubInstallationDetailsResponse getInstallationDetails(Long installationId) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }

        String jwtToken = githubJwtService.generateAppJwt();

        try {
            return restClient.get()
                    .uri("/app/installations/{installationId}", installationId)
                    .header("Authorization", "Bearer " + jwtToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            throw new GithubApiException("GitHub API authentication/authorization failed (HTTP " + status + ")", status);
                        } else if (status == 404) {
                            throw new ResourceNotFoundException("GitHub installation not found on GitHub with ID: " + installationId);
                        } else {
                            throw new GithubApiException("GitHub API request failed with status HTTP " + status, status);
                        }
                    })
                    .body(GithubInstallationDetailsResponse.class);
        } catch (GithubApiException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GithubApiException("Failed to communicate with GitHub API", 500, e);
        }
    }
}
