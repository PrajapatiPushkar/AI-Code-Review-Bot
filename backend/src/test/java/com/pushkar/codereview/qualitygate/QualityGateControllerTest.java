package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.qualitygate.dto.QualityGateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QualityGateControllerTest {

    private MockMvc mockMvc;
    private StubQualityGateService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubQualityGateService();
        QualityGateController controller = new QualityGateController(stubService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetQualityGate_AuthorizedReview_Returns200OK() throws Exception {
        QualityGateResponse response = new QualityGateResponse(
                1L, 42L, QualityGateStatus.PASS, true,
                ReviewFindingSeverity.HIGH, 0,
                "Quality gate passed: 0 findings met or exceeded the HIGH severity threshold.",
                Instant.now()
        );
        stubService.setResponse(response);

        mockMvc.perform(get("/api/v1/code-reviews/42/quality-gate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(42))
                .andExpect(jsonPath("$.status").value("PASS"))
                .andExpect(jsonPath("$.failOnSeverity").value("HIGH"))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testGetQualityGate_WithoutApiV1Prefix_Returns200OK() throws Exception {
        QualityGateResponse response = new QualityGateResponse(
                2L, 42L, QualityGateStatus.FAIL, true,
                ReviewFindingSeverity.HIGH, 2,
                "Quality gate failed: 2 finding(s) met or exceeded the HIGH severity threshold.",
                Instant.now()
        );
        stubService.setResponse(response);

        mockMvc.perform(get("/code-reviews/42/quality-gate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(42))
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.failureCount").value(2));
    }

    @Test
    void testGetQualityGate_UnauthorizedReview_Returns403Forbidden() throws Exception {
        stubService.setException(new AccessDeniedException("You do not have permission to access this code review"));

        mockMvc.perform(get("/api/v1/code-reviews/42/quality-gate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this code review"));
    }

    @Test
    void testGetQualityGate_NotFoundReview_Returns404NotFound() throws Exception {
        stubService.setException(new ResourceNotFoundException("CodeReview record not found with id: 999"));

        mockMvc.perform(get("/api/v1/code-reviews/999/quality-gate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("CodeReview record not found with id: 999"));
    }

    // --- Stub Helper ---

    private static class StubQualityGateService extends QualityGateService {
        private QualityGateResponse response;
        private RuntimeException exception;

        public StubQualityGateService() {
            super(null, null, null, null, null, null);
        }

        void setResponse(QualityGateResponse response) { this.response = response; }
        void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public QualityGateResponse getOrEvaluateQualityGate(Long reviewId) {
            if (exception != null) throw exception;
            return response;
        }
    }
}
