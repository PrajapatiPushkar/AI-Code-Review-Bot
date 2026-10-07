package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.PatchValidationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.fix.CodeFixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeFixControllerTest {

    private MockMvc mockMvc;
    private StubCodeFixService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubCodeFixService();
        CodeFixController controller = new CodeFixController(stubService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGenerateProposedFix_Success_Returns200OK() throws Exception {
        CodeFixResponse response = new CodeFixResponse(
                10L,
                "src/main/ActivityService.java",
                "Fixed NPE check",
                "--- a/src/main/ActivityService.java\n+++ b/src/main/ActivityService.java\n@@ -42 +42 @@\n-old\n+new",
                "old",
                "new",
                Instant.parse("2026-10-07T06:00:00Z"),
                "Gemini",
                "PROPOSED",
                null
        );

        stubService.setResponse(response);

        mockMvc.perform(post("/api/v1/code-reviews/findings/10/fix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructions\": \"Make minimal change\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingId").value(10))
                .andExpect(jsonPath("$.filePath").value("src/main/ActivityService.java"))
                .andExpect(jsonPath("$.explanation").value("Fixed NPE check"))
                .andExpect(jsonPath("$.unifiedDiff").exists())
                .andExpect(jsonPath("$.status").value("PROPOSED"));
    }

    @Test
    void testGenerateProposedFix_WithoutRequestBody_Returns200OK() throws Exception {
        CodeFixResponse response = new CodeFixResponse(
                10L,
                "src/main/ActivityService.java",
                "Fixed NPE check",
                "--- a/src/main/ActivityService.java\n+++ b/src/main/ActivityService.java\n@@ -42 +42 @@\n-old\n+new",
                "old",
                "new",
                Instant.now(),
                "Gemini",
                "PROPOSED",
                null
        );

        stubService.setResponse(response);

        mockMvc.perform(post("/code-reviews/findings/10/fix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingId").value(10));
    }

    @Test
    void testGenerateProposedFix_NotFound_Returns404() throws Exception {
        stubService.setException(new ResourceNotFoundException("Review finding not found with id: 999"));

        mockMvc.perform(post("/api/v1/code-reviews/findings/999/fix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("Review finding not found with id: 999"));
    }

    @Test
    void testGenerateProposedFix_Unauthorized_Returns403() throws Exception {
        stubService.setException(new AccessDeniedException("Access denied for this finding"));

        mockMvc.perform(post("/api/v1/code-reviews/findings/10/fix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testGenerateProposedFix_PatchValidationFailed_Returns400() throws Exception {
        stubService.setException(new PatchValidationException("Path traversal detected in patch candidate"));

        mockMvc.perform(post("/api/v1/code-reviews/findings/10/fix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Patch Validation Failed"))
                .andExpect(jsonPath("$.message").value("Path traversal detected in patch candidate"));
    }

    private static class StubCodeFixService extends CodeFixService {
        private CodeFixResponse response;
        private RuntimeException exception;

        public StubCodeFixService() {
            super(null, null, null, null);
        }

        public void setResponse(CodeFixResponse response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public CodeFixResponse generateFix(Long findingId, CodeFixRequest request) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
