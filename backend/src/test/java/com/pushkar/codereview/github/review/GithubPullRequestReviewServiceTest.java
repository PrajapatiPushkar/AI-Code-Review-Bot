package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.GithubPullRequestClient;
import com.pushkar.codereview.github.client.GithubPullRequestFilesClient;
import com.pushkar.codereview.github.client.GithubRepositoryClient;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubPullRequestReviewServiceTest {

    private StubRepositoryClient repositoryClient;
    private StubPullRequestClient pullRequestClient;
    private StubPullRequestFilesClient filesClient;
    private GithubPullRequestReviewService reviewService;

    private static final Long INSTALLATION_ID = 12345L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final int PR_NUMBER = 42;

    @BeforeEach
    void setUp() {
        repositoryClient = new StubRepositoryClient();
        pullRequestClient = new StubPullRequestClient();
        filesClient = new StubPullRequestFilesClient();
        reviewService = new GithubPullRequestReviewService(repositoryClient, pullRequestClient, filesClient);
    }

    @Test
    void testGetReviewContext_Success() {
        GithubRepositoryResponse repoResponse = new GithubRepositoryResponse(1L, REPO, OWNER + "/" + REPO, false, "https://github.com/octocat/hello-world", "main");
        GithubPullRequestResponse prResponse = new GithubPullRequestResponse(
                100L, (long) PR_NUMBER, "Test PR", "Description", "open",
                "https://github.com/octocat/hello-world/pull/42",
                new GithubPullRequestResponse.UserResponse(OWNER),
                new GithubPullRequestResponse.GitRefResponse("feature-branch"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                Instant.now(), Instant.now()
        );
        GithubPullRequestFileResponse fileResponse = new GithubPullRequestFileResponse(
                "sha123", "src/Main.java", "modified", 10, 2, 12, "@@ -1,2 +1,2 @@", null
        );
        List<GithubPullRequestFileResponse> filesResponse = List.of(fileResponse);

        repositoryClient.setResponse(repoResponse);
        pullRequestClient.setResponse(prResponse);
        filesClient.setResponse(filesResponse);

        PullRequestReviewContext context = reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(context).isNotNull();
        assertThat(context.getRepository()).isEqualTo(repoResponse);
        assertThat(context.getPullRequest()).isEqualTo(prResponse);
        assertThat(context.getChangedFiles()).hasSize(1);
        assertThat(context.getChangedFiles().get(0)).isEqualTo(fileResponse);

        assertThat(repositoryClient.isCalled()).isTrue();
        assertThat(pullRequestClient.isCalled()).isTrue();
        assertThat(filesClient.isCalled()).isTrue();
    }

    @Test
    void testGetReviewContext_RepositoryClientFailure() {
        repositoryClient.setException(new ResourceNotFoundException("GitHub repository not found: " + OWNER + "/" + REPO));

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub repository not found");

        assertThat(repositoryClient.isCalled()).isTrue();
        assertThat(pullRequestClient.isCalled()).isFalse();
        assertThat(filesClient.isCalled()).isFalse();
    }

    @Test
    void testGetReviewContext_PullRequestClientFailure() {
        GithubRepositoryResponse repoResponse = new GithubRepositoryResponse(1L, REPO, OWNER + "/" + REPO, false, "https://github.com/octocat/hello-world", "main");
        repositoryClient.setResponse(repoResponse);
        pullRequestClient.setException(new GithubApiException("GitHub pull request not found", 404));

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub pull request not found");

        assertThat(repositoryClient.isCalled()).isTrue();
        assertThat(pullRequestClient.isCalled()).isTrue();
        assertThat(filesClient.isCalled()).isFalse();
    }

    @Test
    void testGetReviewContext_ChangedFilesClientFailure() {
        GithubRepositoryResponse repoResponse = new GithubRepositoryResponse(1L, REPO, OWNER + "/" + REPO, false, "https://github.com/octocat/hello-world", "main");
        GithubPullRequestResponse prResponse = new GithubPullRequestResponse(
                100L, (long) PR_NUMBER, "Test PR", "Description", "open",
                "https://github.com/octocat/hello-world/pull/42",
                new GithubPullRequestResponse.UserResponse(OWNER),
                new GithubPullRequestResponse.GitRefResponse("feature-branch"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                Instant.now(), Instant.now()
        );

        repositoryClient.setResponse(repoResponse);
        pullRequestClient.setResponse(prResponse);
        filesClient.setException(new GithubApiException("GitHub API server error", 502));

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("GitHub API server error");

        assertThat(repositoryClient.isCalled()).isTrue();
        assertThat(pullRequestClient.isCalled()).isTrue();
        assertThat(filesClient.isCalled()).isTrue();
    }

    @Test
    void testGetReviewContext_EmptyChangedFiles() {
        GithubRepositoryResponse repoResponse = new GithubRepositoryResponse(1L, REPO, OWNER + "/" + REPO, false, "https://github.com/octocat/hello-world", "main");
        GithubPullRequestResponse prResponse = new GithubPullRequestResponse(
                100L, (long) PR_NUMBER, "Test PR", "Description", "open",
                "https://github.com/octocat/hello-world/pull/42",
                new GithubPullRequestResponse.UserResponse(OWNER),
                new GithubPullRequestResponse.GitRefResponse("feature-branch"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                Instant.now(), Instant.now()
        );

        repositoryClient.setResponse(repoResponse);
        pullRequestClient.setResponse(prResponse);
        filesClient.setResponse(Collections.emptyList());

        PullRequestReviewContext context = reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(context).isNotNull();
        assertThat(context.getRepository()).isEqualTo(repoResponse);
        assertThat(context.getPullRequest()).isEqualTo(prResponse);
        assertThat(context.getChangedFiles()).isNotNull().isEmpty();
    }

    @Test
    void testGetReviewContext_NullChangedFilesHandledGracefully() {
        GithubRepositoryResponse repoResponse = new GithubRepositoryResponse(1L, REPO, OWNER + "/" + REPO, false, "https://github.com/octocat/hello-world", "main");
        GithubPullRequestResponse prResponse = new GithubPullRequestResponse(
                100L, (long) PR_NUMBER, "Test PR", "Description", "open",
                "https://github.com/octocat/hello-world/pull/42",
                new GithubPullRequestResponse.UserResponse(OWNER),
                new GithubPullRequestResponse.GitRefResponse("feature-branch"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                Instant.now(), Instant.now()
        );

        repositoryClient.setResponse(repoResponse);
        pullRequestClient.setResponse(prResponse);
        filesClient.setResponse(null);

        PullRequestReviewContext context = reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, PR_NUMBER);

        assertThat(context).isNotNull();
        assertThat(context.getChangedFiles()).isNotNull().isEmpty();
    }

    @Test
    void testGetReviewContext_InvalidInput() {
        // Invalid installation ID
        assertThatThrownBy(() -> reviewService.getReviewContext(null, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> reviewService.getReviewContext(0L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        assertThatThrownBy(() -> reviewService.getReviewContext(-1L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installation ID must be a positive number");

        // Blank owner
        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, null, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository owner must not be blank");

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, "   ", REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository owner must not be blank");

        // Blank repository
        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, null, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository name must not be blank");

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, " ", PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository name must not be blank");

        // Invalid PR number
        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");

        assertThatThrownBy(() -> reviewService.getReviewContext(INSTALLATION_ID, OWNER, REPO, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be a positive number");

        assertThat(repositoryClient.isCalled()).isFalse();
        assertThat(pullRequestClient.isCalled()).isFalse();
        assertThat(filesClient.isCalled()).isFalse();
    }

    // --- Helper Stub Classes ---

    private static class StubRepositoryClient extends GithubRepositoryClient {
        private GithubRepositoryResponse response;
        private RuntimeException exception;
        private boolean called = false;

        public StubRepositoryClient() {
            super((RestClient) null, null);
        }

        public void setResponse(GithubRepositoryResponse response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public boolean isCalled() {
            return called;
        }

        @Override
        public GithubRepositoryResponse getRepository(Long installationId, String owner, String repository) {
            this.called = true;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }

    private static class StubPullRequestClient extends GithubPullRequestClient {
        private GithubPullRequestResponse response;
        private RuntimeException exception;
        private boolean called = false;

        public StubPullRequestClient() {
            super((RestClient) null, null);
        }

        public void setResponse(GithubPullRequestResponse response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public boolean isCalled() {
            return called;
        }

        @Override
        public GithubPullRequestResponse getPullRequest(Long installationId, String owner, String repository, long pullRequestNumber) {
            this.called = true;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }

    private static class StubPullRequestFilesClient extends GithubPullRequestFilesClient {
        private List<GithubPullRequestFileResponse> response;
        private RuntimeException exception;
        private boolean called = false;

        public StubPullRequestFilesClient() {
            super((RestClient) null, null);
        }

        public void setResponse(List<GithubPullRequestFileResponse> response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public boolean isCalled() {
            return called;
        }

        @Override
        public List<GithubPullRequestFileResponse> getChangedFiles(Long installationId, String owner, String repository, long pullRequestNumber) {
            this.called = true;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
