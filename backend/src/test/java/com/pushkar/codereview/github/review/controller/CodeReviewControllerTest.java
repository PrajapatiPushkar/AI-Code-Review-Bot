package com.pushkar.codereview.github.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.CodeReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeReviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private StubCodeReviewService stubCodeReviewService;

    @BeforeEach
    void setUp() {
        stubCodeReviewService = new StubCodeReviewService();
        CodeReviewController controller = new CodeReviewController(stubCodeReviewService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void testReviewPullRequest_Success() throws Exception {
        CodeReviewRequest request = new CodeReviewRequest(123456L, "octocat", "hello-world", 42);
        CodeReviewExecutionResult expectedResult = new CodeReviewExecutionResult(
                "octocat/hello-world", 42L, "AI review completed cleanly.", 2, 2
        );
        stubCodeReviewService.setResult(expectedResult);

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repository").value("octocat/hello-world"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42))
                .andExpect(jsonPath("$.reviewSummary").value("AI review completed cleanly."))
                .andExpect(jsonPath("$.totalFindings").value(2))
                .andExpect(jsonPath("$.postedCommentsCount").value(2));

        assertThat(stubCodeReviewService.isCalled()).isTrue();
        assertThat(stubCodeReviewService.getReceivedInstallationId()).isEqualTo(123456L);
        assertThat(stubCodeReviewService.getReceivedOwner()).isEqualTo("octocat");
        assertThat(stubCodeReviewService.getReceivedRepository()).isEqualTo("hello-world");
        assertThat(stubCodeReviewService.getReceivedPullRequestNumber()).isEqualTo(42);
    }

    @Test
    void testReviewPullRequest_MissingOrInvalidInstallationId() throws Exception {
        CodeReviewRequest requestNull = new CodeReviewRequest(null, "octocat", "hello-world", 42);
        CodeReviewRequest requestNegative = new CodeReviewRequest(-5L, "octocat", "hello-world", 42);

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestNull)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.installationId").exists());

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestNegative)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.installationId").exists());

        assertThat(stubCodeReviewService.isCalled()).isFalse();
    }

    @Test
    void testReviewPullRequest_BlankOwner() throws Exception {
        CodeReviewRequest requestBlankOwner = new CodeReviewRequest(123456L, "   ", "hello-world", 42);

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBlankOwner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.owner").exists());

        assertThat(stubCodeReviewService.isCalled()).isFalse();
    }

    @Test
    void testReviewPullRequest_BlankRepository() throws Exception {
        CodeReviewRequest requestBlankRepo = new CodeReviewRequest(123456L, "octocat", "", 42);

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBlankRepo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.repository").exists());

        assertThat(stubCodeReviewService.isCalled()).isFalse();
    }

    @Test
    void testReviewPullRequest_InvalidPullRequestNumber() throws Exception {
        CodeReviewRequest requestNullPr = new CodeReviewRequest(123456L, "octocat", "hello-world", null);
        CodeReviewRequest requestZeroPr = new CodeReviewRequest(123456L, "octocat", "hello-world", 0);

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestNullPr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.pullRequestNumber").exists());

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestZeroPr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.pullRequestNumber").exists());

        assertThat(stubCodeReviewService.isCalled()).isFalse();
    }

    @Test
    void testReviewPullRequest_ResourceNotFoundExceptionPropagated() throws Exception {
        CodeReviewRequest request = new CodeReviewRequest(123456L, "octocat", "hello-world", 42);
        stubCodeReviewService.setException(new ResourceNotFoundException("GitHub PR not found"));

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("GitHub PR not found"));
    }

    @Test
    void testReviewPullRequest_GithubApiExceptionPropagated() throws Exception {
        CodeReviewRequest request = new CodeReviewRequest(123456L, "octocat", "hello-world", 42);
        stubCodeReviewService.setException(new GithubApiException("GitHub API rate limit exceeded", 429));

        mockMvc.perform(post("/api/v1/code-reviews/pull-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("GitHub API Error"))
                .andExpect(jsonPath("$.message").value("GitHub API rate limit exceeded"));
    }

    // --- Helper Stub ---

    private static class StubCodeReviewService extends GithubPullRequestCodeReviewService {
        private CodeReviewExecutionResult result;
        private RuntimeException exception;
        private boolean called = false;
        private Long receivedInstallationId;
        private String receivedOwner;
        private String receivedRepository;
        private long receivedPullRequestNumber;

        public StubCodeReviewService() {
            super(null, null, null);
        }

        public void setResult(CodeReviewExecutionResult result) {
            this.result = result;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        public boolean isCalled() {
            return called;
        }

        public Long getReceivedInstallationId() {
            return receivedInstallationId;
        }

        public String getReceivedOwner() {
            return receivedOwner;
        }

        public String getReceivedRepository() {
            return receivedRepository;
        }

        public long getReceivedPullRequestNumber() {
            return receivedPullRequestNumber;
        }

        @Override
        public CodeReviewExecutionResult executeCodeReview(Long installationId, String owner, String repository, int pullRequestNumber) {
            return executeCodeReview(installationId, owner, repository, (long) pullRequestNumber);
        }

        @Override
        public CodeReviewExecutionResult executeCodeReview(Long installationId, String owner, String repository, long pullRequestNumber) {
            this.called = true;
            this.receivedInstallationId = installationId;
            this.receivedOwner = owner;
            this.receivedRepository = repository;
            this.receivedPullRequestNumber = pullRequestNumber;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
