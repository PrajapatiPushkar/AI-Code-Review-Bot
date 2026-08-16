package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.CodeReviewHistoryService;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeReviewHistoryControllerTest {

    private MockMvc mockMvc;
    private StubCodeReviewHistoryService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubCodeReviewHistoryService();
        CodeReviewHistoryController controller = new CodeReviewHistoryController(stubService);

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCodeReviews_Paginated_Success() throws Exception {
        CodeReviewHistoryResponse sample = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Found 2 potential issues.", 2, 2,
                CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"),
                Instant.parse("2026-08-16T05:00:12Z")
        );
        Page<CodeReviewHistoryResponse> pageResponse = new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1);
        stubService.setPageResponse(pageResponse);

        mockMvc.perform(get("/api/v1/code-reviews?page=0&size=20&sort=createdAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].owner").value("octocat"))
                .andExpect(jsonPath("$.content[0].repository").value("hello-world"))
                .andExpect(jsonPath("$.content[0].pullRequestNumber").value(42))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetCodeReviews_InvalidStatusFilter_Returns400() throws Exception {
        stubService.setException(new IllegalArgumentException("Invalid status filter: INVALID_STATUS"));

        mockMvc.perform(get("/api/v1/code-reviews?status=INVALID_STATUS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid status filter: INVALID_STATUS"));
    }

    @Test
    void testGetSingleReview_Success() throws Exception {
        CodeReviewHistoryResponse sample = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Found 2 potential issues.", 2, 2,
                CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"),
                Instant.parse("2026-08-16T05:00:12Z")
        );
        stubService.setSingleResponse(sample);

        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.installationId").value(123456))
                .andExpect(jsonPath("$.owner").value("octocat"))
                .andExpect(jsonPath("$.repository").value("hello-world"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42))
                .andExpect(jsonPath("$.reviewSummary").value("Found 2 potential issues."))
                .andExpect(jsonPath("$.totalFindings").value(2))
                .andExpect(jsonPath("$.postedCommentsCount").value(2))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-16T05:00:00Z"))
                .andExpect(jsonPath("$.completedAt").value("2026-08-16T05:00:12Z"));
    }

    @Test
    void testGetRepositoryHistory_Success() throws Exception {
        CodeReviewHistoryResponse r1 = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Summary 1", 2, 2,
                CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"),
                Instant.parse("2026-08-16T05:00:12Z")
        );
        CodeReviewHistoryResponse r2 = new CodeReviewHistoryResponse(
                2L, 123456L, "octocat", "hello-world", 43,
                "Summary 2", 0, 0,
                CodeReviewStatus.IN_PROGRESS,
                Instant.parse("2026-08-16T05:10:00Z"),
                null
        );
        stubService.setListResponse(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/code-reviews/repository/octocat/hello-world")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].owner").value("octocat"))
                .andExpect(jsonPath("$[0].repository").value("hello-world"))
                .andExpect(jsonPath("$[0].pullRequestNumber").value(42))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].pullRequestNumber").value(43))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));
    }

    @Test
    void testGetPullRequestHistory_Success() throws Exception {
        CodeReviewHistoryResponse r1 = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Summary 1", 2, 2,
                CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"),
                Instant.parse("2026-08-16T05:00:12Z")
        );
        stubService.setListResponse(List.of(r1));

        mockMvc.perform(get("/api/v1/code-reviews/repository/octocat/hello-world/pull-request/42")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].pullRequestNumber").value(42))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void testMissingReview_404() throws Exception {
        stubService.setException(new ResourceNotFoundException("CodeReview record not found with id: 999"));

        mockMvc.perform(get("/api/v1/code-reviews/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("CodeReview record not found with id: 999"));
    }

    @Test
    void testInvalidRequestParameters_400() throws Exception {
        stubService.setException(new IllegalArgumentException("Review ID must be positive"));

        mockMvc.perform(get("/api/v1/code-reviews/-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Review ID must be positive"));
    }

    // --- Helper Stub ---

    private static class StubCodeReviewHistoryService extends CodeReviewHistoryService {
        private CodeReviewHistoryResponse singleResponse;
        private List<CodeReviewHistoryResponse> listResponse = List.of();
        private Page<CodeReviewHistoryResponse> pageResponse = Page.empty();
        private RuntimeException exception;

        public StubCodeReviewHistoryService() {
            super(null);
        }

        public void setSingleResponse(CodeReviewHistoryResponse singleResponse) {
            this.singleResponse = singleResponse;
        }

        public void setListResponse(List<CodeReviewHistoryResponse> listResponse) {
            this.listResponse = listResponse;
        }

        public void setPageResponse(Page<CodeReviewHistoryResponse> pageResponse) {
            this.pageResponse = pageResponse;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public Page<CodeReviewHistoryResponse> getCodeReviews(int page, int size, String sort, String statusStr, String owner, String repositoryName, Integer pullRequestNumber) {
            if (exception != null) {
                throw exception;
            }
            return pageResponse;
        }

        @Override
        public CodeReviewHistoryResponse getById(Long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Review ID must be positive");
            }
            if (exception != null) {
                throw exception;
            }
            return singleResponse;
        }

        @Override
        public List<CodeReviewHistoryResponse> getByRepository(String owner, String repositoryName) {
            if (exception != null) {
                throw exception;
            }
            return listResponse;
        }

        @Override
        public List<CodeReviewHistoryResponse> getByPullRequest(String owner, String repositoryName, int pullRequestNumber) {
            if (exception != null) {
                throw exception;
            }
            return listResponse;
        }
    }
}
