package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentRequest;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubPullRequestReviewCommentClientTest {

    private static final Long INSTALLATION_ID = 12345L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final long PR_NUMBER = 42L;
    private static final String TOKEN = "test-installation-token-abc";

    private StubTokenService tokenService;
    private GithubPullRequestReviewCommentClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        tokenService = new StubTokenService();
        tokenService.setToken(TOKEN);

        GithubProperties properties = new GithubProperties();
        properties.setApiBaseUrl("https://api.github.com");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        client = new GithubPullRequestReviewCommentClient(builder.baseUrl(properties.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build(), tokenService);
    }

    @Test
    void testCreateReviewComment_Success() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest(
                "Comment body", "sha12345", "src/Main.java", 15, "RIGHT"
        );

        String responseJson = """
                {
                  "id": 1001,
                  "body": "Comment body",
                  "path": "src/Main.java",
                  "line": 15,
                  "commit_id": "sha12345",
                  "html_url": "https://github.com/octocat/hello-world/pull/42#discussion_r1001",
                  "created_at": "2026-08-15T12:00:00Z"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {
                          "body": "Comment body",
                          "commit_id": "sha12345",
                          "path": "src/Main.java",
                          "line": 15,
                          "side": "RIGHT"
                        }
                        """))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubReviewCommentResponse response = client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request);

        mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1001L);
        assertThat(response.getBody()).isEqualTo("Comment body");
        assertThat(response.getPath()).isEqualTo("src/Main.java");
        assertThat(response.getLine()).isEqualTo(15);
        assertThat(response.getCommitId()).isEqualTo("sha12345");
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/octocat/hello-world/pull/42#discussion_r1001");
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-08-15T12:00:00Z"));
    }

    @Test
    void testCreateReviewComment_401Unauthorized() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API authentication/authorization failed (HTTP 401)");
    }

    @Test
    void testCreateReviewComment_403Forbidden() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API authentication/authorization failed (HTTP 403)");
    }

    @Test
    void testCreateReviewComment_404NotFound() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub pull request not found");
    }

    @Test
    void testCreateReviewComment_422UnprocessableEntity() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API comment validation failed (HTTP 422)");
    }

    @Test
    void testCreateReviewComment_500ServerError() {
        GithubReviewCommentRequest request = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/hello-world/pulls/42/comments"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, request))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API server error (HTTP 500)");
    }

    @Test
    void testCreateReviewComment_InvalidInputs() {
        GithubReviewCommentRequest validRequest = new GithubReviewCommentRequest("body", "sha", "file.java", 1);

        // Invalid installationId
        assertThatThrownBy(() -> client.createReviewComment(null, OWNER, REPO, PR_NUMBER, validRequest))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.createReviewComment(0L, OWNER, REPO, PR_NUMBER, validRequest))
                .isInstanceOf(IllegalArgumentException.class);

        // Blank owner/repo
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, " ", REPO, PR_NUMBER, validRequest))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, "", PR_NUMBER, validRequest))
                .isInstanceOf(IllegalArgumentException.class);

        // Invalid PR number
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, 0, validRequest))
                .isInstanceOf(IllegalArgumentException.class);

        // Null request
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, null))
                .isInstanceOf(IllegalArgumentException.class);

        // Invalid request fields
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, new GithubReviewCommentRequest(null, "sha", "file.java", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, new GithubReviewCommentRequest("body", "", "file.java", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, new GithubReviewCommentRequest("body", "sha", "  ", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.createReviewComment(INSTALLATION_ID, OWNER, REPO, PR_NUMBER, new GithubReviewCommentRequest("body", "sha", "file.java", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Helper Stub ---

    private static class StubTokenService extends GithubInstallationTokenService {
        private String token;

        public StubTokenService() {
            super(null);
        }

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public String getInstallationAccessToken(Long installationId) {
            return token;
        }
    }
}
