package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.PatchValidationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeFixProposalResponse;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.fix.CodeFixProposalStatus;
import com.pushkar.codereview.github.review.fix.CodeFixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                100L,
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
                .andExpect(jsonPath("$.proposalId").value(100))
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

    @Test
    void testGetFindingProposals_Returns200OK() throws Exception {
        CodeFixProposalResponse p1 = new CodeFixProposalResponse(
                50L, 10L, "src/Test.java", "Fix NPE", "diff", "old", "new",
                "instructions", "Gemini", "gemini-3.6-flash", CodeFixProposalStatus.PROPOSED,
                Instant.now(), Instant.now()
        );
        stubService.setProposalResponses(List.of(p1));

        mockMvc.perform(get("/api/v1/code-reviews/findings/10/fixes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].findingId").value(10))
                .andExpect(jsonPath("$[0].status").value("PROPOSED"));
    }

    @Test
    void testGetFindingProposals_Unauthorized_Returns403() throws Exception {
        stubService.setException(new AccessDeniedException("Access denied for this finding"));

        mockMvc.perform(get("/api/v1/code-reviews/findings/10/fixes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testGetProposal_Success_Returns200OK() throws Exception {
        CodeFixProposalResponse proposal = new CodeFixProposalResponse(
                50L, 10L, "src/Test.java", "Fix NPE", "diff", "old", "new",
                "instructions", "Gemini", "gemini-3.6-flash", CodeFixProposalStatus.PROPOSED,
                Instant.now(), Instant.now()
        );
        stubService.setSingleProposalResponse(proposal);

        mockMvc.perform(get("/api/v1/code-reviews/fixes/50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.explanation").value("Fix NPE"))
                .andExpect(jsonPath("$.status").value("PROPOSED"));
    }

    @Test
    void testGetProposal_Unauthorized_Returns403() throws Exception {
        stubService.setException(new AccessDeniedException("Access denied for this fix proposal"));

        mockMvc.perform(get("/api/v1/code-reviews/fixes/50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testGetProposal_NotFound_Returns404() throws Exception {
        stubService.setException(new ResourceNotFoundException("Fix proposal not found with id: 999"));

        mockMvc.perform(get("/api/v1/code-reviews/fixes/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void testUpdateProposalStatus_ValidTransition_Returns200OK() throws Exception {
        CodeFixProposalResponse proposal = new CodeFixProposalResponse(
                50L, 10L, "src/Test.java", "Fix NPE", "diff", "old", "new",
                "instructions", "Gemini", "gemini-3.6-flash", CodeFixProposalStatus.REVIEWED,
                Instant.now(), Instant.now()
        );
        stubService.setSingleProposalResponse(proposal);

        mockMvc.perform(patch("/api/v1/code-reviews/fixes/50/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REVIEWED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("REVIEWED"));
    }

    @Test
    void testUpdateProposalStatus_InvalidTransition_Returns400BadRequest() throws Exception {
        stubService.setException(new IllegalArgumentException("Invalid status transition from REJECTED to REVIEWED"));

        mockMvc.perform(patch("/api/v1/code-reviews/fixes/50/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REVIEWED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid status transition from REJECTED to REVIEWED"));
    }

    private static class StubCodeFixService extends CodeFixService {
        private CodeFixResponse response;
        private List<CodeFixProposalResponse> proposalResponses;
        private CodeFixProposalResponse singleProposalResponse;
        private RuntimeException exception;

        public StubCodeFixService() {
            super(null, null, null, null);
        }

        public void setResponse(CodeFixResponse response) {
            this.response = response;
        }

        public void setProposalResponses(List<CodeFixProposalResponse> proposalResponses) {
            this.proposalResponses = proposalResponses;
        }

        public void setSingleProposalResponse(CodeFixProposalResponse singleProposalResponse) {
            this.singleProposalResponse = singleProposalResponse;
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

        @Override
        public List<CodeFixProposalResponse> getFindingProposals(Long findingId) {
            if (exception != null) {
                throw exception;
            }
            return proposalResponses != null ? proposalResponses : List.of();
        }

        @Override
        public CodeFixProposalResponse getProposal(Long proposalId) {
            if (exception != null) {
                throw exception;
            }
            return singleProposalResponse;
        }

        @Override
        public CodeFixProposalResponse updateProposalStatus(Long proposalId, CodeFixProposalStatus targetStatus) {
            if (exception != null) {
                throw exception;
            }
            return singleProposalResponse;
        }
    }
}
