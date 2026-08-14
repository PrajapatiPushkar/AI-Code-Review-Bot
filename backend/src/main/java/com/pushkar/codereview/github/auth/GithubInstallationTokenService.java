package com.pushkar.codereview.github.auth;

import com.pushkar.codereview.github.client.GithubInstallationTokenClient;
import com.pushkar.codereview.github.client.dto.GithubInstallationTokenResponse;
import org.springframework.stereotype.Service;

@Service
public class GithubInstallationTokenService {

    private final GithubInstallationTokenClient githubInstallationTokenClient;

    public GithubInstallationTokenService(GithubInstallationTokenClient githubInstallationTokenClient) {
        this.githubInstallationTokenClient = githubInstallationTokenClient;
    }

    public GithubInstallationTokenResponse getInstallationAccessTokenResponse(Long installationId) {
        return githubInstallationTokenClient.requestInstallationToken(installationId);
    }

    public String getInstallationAccessToken(Long installationId) {
        GithubInstallationTokenResponse response = getInstallationAccessTokenResponse(installationId);
        return response != null ? response.getToken() : null;
    }
}
