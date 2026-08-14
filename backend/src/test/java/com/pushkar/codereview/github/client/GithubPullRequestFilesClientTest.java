package com.pushkar.codereview.github.client;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.auth.GithubInstallationTokenService;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubPullRequestFilesClientTest {

    private MockRestServiceServer mockServer;
    private GithubPullRequestFilesClient filesClient;

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

        filesClient = new GithubPullRequestFilesClient(githubProperties, stubTokenService, builder);
    }

    @Test
    void testGetChangedFiles_SinglePage() {
        String responseJson = """
                [
                  {
                    "sha": "bb0ae2941d4aef7708c2a39d04899a7127075d9b",
                    "filename": "file1.txt",
                    "status": "added",
                    "additions": 10,
                    "deletions": 2,
                    "changes": 12,
                    "patch": "@@ -0,0 +1,10 @@\\n+Hello World",
                    "previous_filename": null
                  },
                  {
                    "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                    "filename": "file2.txt",
                    "status": "modified",
                    "additions": 5,
                    "deletions": 1,
                    "changes": 6,
                    "patch": "@@ -1,3 +1,5 @@\\n-old line\\n+new line",
                    "previous_filename": null
                  }
                ]
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<GithubPullRequestFileResponse> files = filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(files).hasSize(2);
        assertThat(files.get(0).getFilename()).isEqualTo("file1.txt");
        assertThat(files.get(0).getStatus()).isEqualTo("added");
        assertThat(files.get(0).getAdditions()).isEqualTo(10);
        assertThat(files.get(0).getPatch()).contains("+Hello World");
        assertThat(files.get(1).getFilename()).isEqualTo("file2.txt");

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_MultiplePages() {
        StringBuilder page1Builder = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) page1Builder.append(",");
            page1Builder.append(String.format("""
                    {
                      "sha": "sha_%d",
                      "filename": "file_%d.txt",
                      "status": "modified",
                      "additions": 1,
                      "deletions": 0,
                      "changes": 1,
                      "patch": "@@ -1 +1 @@"
                    }
                    """, i, i));
        }
        page1Builder.append("]");

        String page2Json = """
                [
                  {
                    "sha": "sha_100",
                    "filename": "file_100.txt",
                    "status": "modified",
                    "additions": 1,
                    "deletions": 0,
                    "changes": 1,
                    "patch": "@@ -1 +1 @@"
                  }
                ]
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess(page1Builder.toString(), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=2&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess(page2Json, MediaType.APPLICATION_JSON));

        List<GithubPullRequestFileResponse> files = filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(files).hasSize(101);
        assertThat(files.get(0).getFilename()).isEqualTo("file_0.txt");
        assertThat(files.get(100).getFilename()).isEqualTo("file_100.txt");

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_EmptyPage() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GithubPullRequestFileResponse> files = filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(files).isEmpty();
        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_NullPatch() {
        String responseJson = """
                [
                  {
                    "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                    "filename": "image.png",
                    "status": "added",
                    "additions": 0,
                    "deletions": 0,
                    "changes": 0,
                    "patch": null
                  }
                ]
                """;

        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<GithubPullRequestFileResponse> files = filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFilename()).isEqualTo("image.png");
        assertThat(files.get(0).getPatch()).isNull();

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_MaxPagesExceeded() {
        StringBuilder pageBuilder = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) pageBuilder.append(",");
            pageBuilder.append(String.format("""
                    {
                      "sha": "sha_%d",
                      "filename": "file_%d.txt",
                      "status": "modified",
                      "additions": 1,
                      "deletions": 0,
                      "changes": 1,
                      "patch": "@@ -1 +1 @@"
                    }
                    """, i, i));
        }
        pageBuilder.append("]");

        for (int page = 1; page <= 30; page++) {
            mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=" + page + "&per_page=100"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(pageBuilder.toString(), MediaType.APPLICATION_JSON));
        }

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("Exceeded maximum page limit (30)");

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_Unauthorized_401() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_Forbidden_403() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_NotFound_404() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub pull request not found: octocat/hello-world#1347")
                .hasMessageNotContaining(MOCK_TOKEN);

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_ServerError_500() {
        mockServer.expect(requestTo(BASE_URL + "/repos/octocat/hello-world/pulls/1347/files?page=1&per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .extracting("statusCode")
                .isEqualTo(502);

        mockServer.verify();
    }

    @Test
    void testGetChangedFiles_InvalidInputs() {
        assertThatThrownBy(() -> filesClient.getChangedFiles(null, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> filesClient.getChangedFiles(0L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, "", REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository owner must not be blank");

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, "", PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository name must not be blank");

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");

        assertThatThrownBy(() -> filesClient.getChangedFiles(INSTALLATION_ID, OWNER, REPO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");
    }
}
