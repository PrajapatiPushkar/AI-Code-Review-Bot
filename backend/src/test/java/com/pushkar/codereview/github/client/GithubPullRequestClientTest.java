package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubPullRequestClientTest {

    private MockRestServiceServer mockServer;
    private GithubPullRequestClient pullRequestClient;

    private static final String MOCK_TOKEN = "ghs_16C7e42F292c6912E7710c838347Ae178B4a";
    private static final String BASE_URL = "https://api.github.com";
    private static final Long INSTALLATION_ID = 123456L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final long PR_NUMBER = 1347L;

    @BeforeEach
    void setUp() {
        GithubProperties githubProperties = new GithubProperties();
        githubProperties.setApiBaseUrl(BASE_URL);

        GithubInstallationTokenService stubTokenService = new GithubInstallationTokenService(null) {
            @Override
            public String getInstallationAccessToken(Long installationId) {
                if (installationId != null && installationId.equals(INSTALLATION_ID)) {
                    return MOCK_TOKEN;
                }
                return "ghs_other_token";
            }
        };

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        pullRequestClient = new GithubPullRequestClient(githubProperties, stubTokenService, builder);
    }

    @Test
    void testGetPullRequest_Success() {
        String responseJson = """
                {
                  "id": 1,
                  "number": 1347,
                  "title": "Amazing new feature",
                  "body": "Please pull these awesome changes in!",
                  "state": "open",
                  "html_url": "https://github.com/octocat/hello-world/pull/1347",
                  "user": {
                    "login": "octocat"
                  },
                  "head": {
                    "ref": "new-topic"
                  },
                  "base": {
                    "ref": "master"
                  },
                  "created_at": "2026-08-14T10:00:00Z",
                  "updated_at": "2026-08-14T11:00:00Z"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubPullRequestResponse response = pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNumber()).isEqualTo(1347L);
        assertThat(response.getTitle()).isEqualTo("Amazing new feature");
        assertThat(response.getBody()).isEqualTo("Please pull these awesome changes in!");
        assertThat(response.getState()).isEqualTo("open");
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/octocat/hello-world/pull/1347");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getLogin()).isEqualTo("octocat");
        assertThat(response.getHead()).isNotNull();
        assertThat(response.getHead().getRef()).isEqualTo("new-topic");
        assertThat(response.getBase()).isNotNull();
        assertThat(response.getBase().getRef()).isEqualTo("master");
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-08-14T10:00:00Z"));
        assertThat(response.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-14T11:00:00Z"));

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_Success_OverloadedMethod() {
        String responseJson = """
                {
                  "id": 2,
                  "number": 1347,
                  "title": "Bug fix",
                  "body": "Fixing critical bug",
                  "state": "closed",
                  "html_url": "https://github.com/octocat/hello-world/pull/1347",
                  "user": {
                    "login": "octocat"
                  },
                  "head": {
                    "ref": "fix-bug"
                  },
                  "base": {
                    "ref": "main"
                  },
                  "created_at": "2026-08-14T10:00:00Z",
                  "updated_at": "2026-08-14T11:00:00Z"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubPullRequestResponse response = pullRequestClient.getPullRequest(OWNER, REPO, PR_NUMBER, INSTALLATION_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getState()).isEqualTo("closed");
        assertThat(response.getHead().getRef()).isEqualTo("fix-bug");

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_Unauthorized_401() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_Forbidden_403() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_NotFound_404() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub pull request not found: octocat/hello-world#1347")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_ServerError_500() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .extracting("statusCode")
                .isEqualTo(502);

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_BadGateway_502() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .extracting("statusCode")
                .isEqualTo(502);

        mockServer.verify();
    }

    @Test
    void testGetPullRequest_InvalidInputs() {
        assertThatThrownBy(() -> pullRequestClient.getPullRequest(null, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(0L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, "", REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository owner must not be blank");

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, "", PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository name must not be blank");

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");

        assertThatThrownBy(() -> pullRequestClient.getPullRequest(INSTALLATION_ID, OWNER, REPO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");
    }
}
