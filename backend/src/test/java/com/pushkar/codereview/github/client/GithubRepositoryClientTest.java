package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubRepositoryClientTest {

    private MockRestServiceServer mockServer;
    private GithubRepositoryClient repositoryClient;

    private static final String MOCK_TOKEN = "ghs_16C7e42F292c6912E7710c838347Ae178B4a";
    private static final String BASE_URL = "https://api.github.com";
    private static final Long INSTALLATION_ID = 123456L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";

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

        repositoryClient = new GithubRepositoryClient(githubProperties, stubTokenService, builder);
    }

    @Test
    void testGetRepository_Success() {
        String responseJson = """
                {
                  "id": 1296269,
                  "name": "hello-world",
                  "full_name": "octocat/hello-world",
                  "private": false,
                  "html_url": "https://github.com/octocat/hello-world",
                  "default_branch": "main"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubRepositoryResponse response = repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1296269L);
        assertThat(response.getName()).isEqualTo("hello-world");
        assertThat(response.getFullName()).isEqualTo("octocat/hello-world");
        assertThat(response.isPrivate()).isFalse();
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/octocat/hello-world");
        assertThat(response.getDefaultBranch()).isEqualTo("main");

        mockServer.verify();
    }

    @Test
    void testGetRepository_Success_OverloadedMethod() {
        String responseJson = """
                {
                  "id": 1296269,
                  "name": "hello-world",
                  "full_name": "octocat/hello-world",
                  "private": true,
                  "html_url": "https://github.com/octocat/hello-world",
                  "default_branch": "master"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GithubRepositoryResponse response = repositoryClient.getRepository(OWNER, REPO, INSTALLATION_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1296269L);
        assertThat(response.isPrivate()).isTrue();
        assertThat(response.getDefaultBranch()).isEqualTo("master");

        mockServer.verify();
    }

    @Test
    void testGetRepository_Unauthorized_401() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetRepository_Forbidden_403() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetRepository_NotFound_404() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub repository not found: octocat/hello-world")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetRepository_ServerError_500() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO))
                .isInstanceOf(GithubApiException.class)
                .extracting("statusCode")
                .isEqualTo(502);

        mockServer.verify();
    }

    @Test
    void testGetRepository_BadGateway_502() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, REPO))
                .isInstanceOf(GithubApiException.class)
                .extracting("statusCode")
                .isEqualTo(502);

        mockServer.verify();
    }

    @Test
    void testGetRepository_InvalidInputs() {
        assertThatThrownBy(() -> repositoryClient.getRepository(null, OWNER, REPO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> repositoryClient.getRepository(0L, OWNER, REPO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, "", REPO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository owner must not be blank");

        assertThatThrownBy(() -> repositoryClient.getRepository(INSTALLATION_ID, OWNER, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository name must not be blank");
    }
}
