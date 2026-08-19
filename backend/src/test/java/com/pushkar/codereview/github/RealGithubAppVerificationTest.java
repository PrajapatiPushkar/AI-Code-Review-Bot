package com.pushkar.codereview.github;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.github.auth.GithubJwtService;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.GithubInstallationTokenClient;
import com.pushkar.codereview.github.client.GithubPullRequestClient;
import com.pushkar.codereview.github.client.GithubPullRequestFilesClient;
import com.pushkar.codereview.github.client.GithubRepositoryClient;
import com.pushkar.codereview.github.client.dto.GithubInstallationRepositoriesResponse;
import com.pushkar.codereview.github.client.dto.GithubInstallationTokenResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.List;

public class RealGithubAppVerificationTest {

    private static final String DEFAULT_KEY_PATH = "C:/Users/kppus/Downloads/pushkar-ai-code-review-bot.2026-08-18.private-key (1).pem";

    static boolean hasKeyFile() {
        String path = System.getenv("GITHUB_PRIVATE_KEY_PATH");
        if (path == null || path.isBlank()) {
            path = DEFAULT_KEY_PATH;
        }
        return new File(path).exists();
    }

    @Test
    @EnabledIf("hasKeyFile")
    public void verifyRealGithubApp() {
        System.out.println("================ REAL GITHUB APP VERIFICATION ================");

        String keyPath = System.getenv("GITHUB_PRIVATE_KEY_PATH");
        if (keyPath == null || keyPath.isBlank()) {
            keyPath = DEFAULT_KEY_PATH;
        }

        GithubProperties props = new GithubProperties();
        props.setAppId(System.getenv("GITHUB_APP_ID") != null ? System.getenv("GITHUB_APP_ID") : "4642046");
        props.setAppName("Pushkar-AI-Code-Review-Bot");
        props.setPrivateKeyPath(keyPath);
        props.setApiBaseUrl("https://api.github.com");

        GithubJwtService jwtService = new GithubJwtService(props);

        // 1. Generate App JWT
        String jwt = jwtService.generateAppJwt();
        System.out.println("[Step 1] GitHub App JWT generated successfully (Length: " + jwt.length() + ")");

        long installationId = 154790187L;

        // 2. Request Installation Access Token
        RestClient.Builder builder = RestClient.builder();
        GithubInstallationTokenClient tokenClient = new GithubInstallationTokenClient(props, jwtService, builder);
        GithubInstallationTokenResponse tokenResp = tokenClient.requestInstallationToken(installationId);
        System.out.println("[Step 2] Installation Access Token acquired successfully for Installation ID " + installationId + "!");

        // 3. List Repositories
        GithubInstallationTokenService tokenService = new GithubInstallationTokenService(tokenClient);
        GithubRepositoryClient repoClient = new GithubRepositoryClient(props, tokenService, builder);
        GithubInstallationRepositoriesResponse reposResp = repoClient.getInstallationRepositories(installationId, 1, 30);

        System.out.println("[Step 3] Accessible Repositories Count: " + (reposResp.getRepositories() != null ? reposResp.getRepositories().size() : 0));
        if (reposResp.getRepositories() != null) {
            for (GithubRepositoryResponse repo : reposResp.getRepositories()) {
                System.out.println(" - Repository: " + repo.getFullName() + " (Private: " + repo.isPrivate() + ", DefaultBranch: " + repo.getDefaultBranch() + ")");

                String owner = repo.getFullName().contains("/") ? repo.getFullName().split("/")[0] : repo.getName();
                String repoName = repo.getName();

                // 4. Fetch Pull Requests
                GithubPullRequestClient prClient = new GithubPullRequestClient(props, tokenService, builder);
                List<GithubPullRequestResponse> prs = prClient.getPullRequests(installationId, owner, repoName, "all", 1, 10);
                System.out.println("   [Step 4] Pull Requests Count in " + repo.getFullName() + ": " + prs.size());

                for (GithubPullRequestResponse pr : prs) {
                    System.out.println("     * PR #" + pr.getNumber() + " [" + pr.getState() + "]: " + pr.getTitle() + " (Head SHA: " + (pr.getHead() != null ? pr.getHead().getSha() : "N/A") + ")");

                    // 5. Fetch Changed Files for PR
                    GithubPullRequestFilesClient filesClient = new GithubPullRequestFilesClient(props, tokenService, builder);
                    List<GithubPullRequestFileResponse> files = filesClient.getChangedFiles(installationId, owner, repoName, pr.getNumber());
                    System.out.println("       Files Changed (" + files.size() + "):");
                    for (GithubPullRequestFileResponse file : files) {
                        System.out.println("        - " + file.getFilename() + " (" + file.getStatus() + ", +" + file.getAdditions() + "/-" + file.getDeletions() + ")");
                    }
                }
            }
        }

        System.out.println("\n================ VERIFICATION FINISHED ================");
    }
}
