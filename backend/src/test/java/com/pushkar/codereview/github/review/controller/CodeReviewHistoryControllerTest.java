package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.CodeReviewHistoryService;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewResultResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewStatusResponse;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeReviewHistoryControllerTest {

    private MockMvc mockMvc;
    private StubCodeReviewHistoryService stubHistoryService;

    @BeforeEach
    void setUp() {
        stubHistoryService = new StubCodeReviewHistoryService();
        CodeReviewHistoryController controller = new CodeReviewHistoryController(stubHistoryService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCodeReviews_Returns200OK() throws Exception {
        CodeReviewHistoryResponse response = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Summary", 2, 2, CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"), Instant.parse("2026-08-16T05:00:12Z")
        );
        stubHistoryService.setPageResponse(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/code-reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].owner").value("octocat"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void testGetById_Returns200OK() throws Exception {
        CodeReviewHistoryResponse response = new CodeReviewHistoryResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                "Summary", 2, 2, CodeReviewStatus.COMPLETED,
                Instant.parse("2026-08-16T05:00:00Z"), Instant.parse("2026-08-16T05:00:12Z")
        );
        stubHistoryService.setSingleResponse(response);

        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.installationId").value(123456))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testGetStatusById_Returns200OK() throws Exception {
        CodeReviewStatusResponse response = new CodeReviewStatusResponse(
                1L, CodeReviewStatus.IN_PROGRESS,
                Instant.parse("2026-08-16T05:00:00Z"), null,
                0, 0, ""
        );
        stubHistoryService.setStatusResponse(response);

        mockMvc.perform(get("/api/v1/code-reviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeReviewId").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void testGetResultById_Returns200OK() throws Exception {
        CodeReviewResultResponse response = new CodeReviewResultResponse(
                1L, 123456L, "octocat", "hello-world", 42,
                CodeReviewStatus.COMPLETED, "Found 2 potential issues.", 2, 2,
                Instant.parse("2026-08-16T05:00:00Z"), Instant.parse("2026-08-16T05:00:12Z")
        );
        stubHistoryService.setResultResponse(response);

        mockMvc.perform(get("/api/v1/code-reviews/1/result")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeReviewId").value(1))
                .andExpect(jsonPath("$.installationId").value(123456))
                .andExpect(jsonPath("$.owner").value("octocat"))
                .andExpect(jsonPath("$.repository").value("hello-world"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reviewSummary").value("Found 2 potential issues."));
    }

    @Test
    void testGetStatusById_AccessDenied_Returns403() throws Exception {
        stubHistoryService.setException(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/code-reviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testGetStatusById_NotFound_Returns404() throws Exception {
        stubHistoryService.setException(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(get("/api/v1/code-reviews/999/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    // --- Helper Stub ---

    private static class StubCodeReviewHistoryService extends CodeReviewHistoryService {
        private CodeReviewHistoryResponse singleResponse;
        private CodeReviewStatusResponse statusResponse;
        private CodeReviewResultResponse resultResponse;
        private org.springframework.data.domain.Page<CodeReviewHistoryResponse> pageResponse = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        private RuntimeException exception;

        public StubCodeReviewHistoryService() {
            super(null);
        }

        public void setSingleResponse(CodeReviewHistoryResponse singleResponse) { this.singleResponse = singleResponse; }
        public void setStatusResponse(CodeReviewStatusResponse statusResponse) { this.statusResponse = statusResponse; }
        public void setResultResponse(CodeReviewResultResponse resultResponse) { this.resultResponse = resultResponse; }
        public void setPageResponse(org.springframework.data.domain.Page<CodeReviewHistoryResponse> pageResponse) { this.pageResponse = pageResponse; }
        public void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public org.springframework.data.domain.Page<CodeReviewHistoryResponse> getCodeReviews(int page, int size, String sort, String status, String owner, String repository, Integer pullRequestNumber) {
            if (exception != null) throw exception;
            return pageResponse;
        }

        @Override
        public CodeReviewHistoryResponse getById(Long id) {
            if (exception != null) throw exception;
            return singleResponse;
        }

        @Override
        public CodeReviewStatusResponse getStatusById(Long id) {
            if (exception != null) throw exception;
            return statusResponse;
        }

        @Override
        public CodeReviewResultResponse getResultById(Long id) {
            if (exception != null) throw exception;
            return resultResponse;
        }
    }
}
