package com.pushkar.codereview.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyRequest;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyResponse;
import com.pushkar.codereview.policy.dto.RuleDefinitionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryPolicyControllerTest {

    private MockMvc mockMvc;
    private StubRepositoryPolicyService stubService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        stubService = new StubRepositoryPolicyService();
        RepositoryPolicyController controller = new RepositoryPolicyController(stubService);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetPolicy_AuthorizedRepo_Returns200OK() throws Exception {
        RepositoryReviewPolicyResponse response = new RepositoryReviewPolicyResponse(
                1L, 10L, "my-repo", "owner/my-repo", true,
                ReviewFindingSeverity.HIGH, Set.of("RULE-JAVA-SYSTEM-OUT"),
                List.of(new RuleDefinitionDto("RULE-JAVA-SYSTEM-OUT", "System.out", "Avoid print", ReviewFindingCategory.CODE_STYLE, ReviewFindingSeverity.LOW)),
                Instant.now(), Instant.now(), true
        );
        stubService.setGetResponse(response);

        mockMvc.perform(get("/api/v1/repositories/10/review-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value(10))
                .andExpect(jsonPath("$.repositoryFullName").value("owner/my-repo"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.failOnSeverity").value("HIGH"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    void testGetPolicy_WithoutApiV1Prefix_Returns200OK() throws Exception {
        RepositoryReviewPolicyResponse response = new RepositoryReviewPolicyResponse(
                1L, 10L, "my-repo", "owner/my-repo", true,
                ReviewFindingSeverity.HIGH, Set.of("RULE-JAVA-SYSTEM-OUT"),
                List.of(), Instant.now(), Instant.now(), false
        );
        stubService.setGetResponse(response);

        mockMvc.perform(get("/repositories/10/review-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value(10));
    }

    @Test
    void testGetPolicy_UnauthorizedRepo_Returns404NotFoundWithoutLeakingExistence() throws Exception {
        stubService.setException(new ResourceNotFoundException("Repository not found with ID: 99"));

        mockMvc.perform(get("/api/v1/repositories/99/review-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Repository not found with ID: 99"));
    }

    @Test
    void testUpdatePolicy_ValidRequest_Returns200OK() throws Exception {
        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true, ReviewFindingSeverity.CRITICAL, Set.of("RULE-JAVA-SYSTEM-OUT")
        );

        RepositoryReviewPolicyResponse response = new RepositoryReviewPolicyResponse(
                1L, 10L, "my-repo", "owner/my-repo", true,
                ReviewFindingSeverity.CRITICAL, Set.of("RULE-JAVA-SYSTEM-OUT"),
                List.of(), Instant.now(), Instant.now(), true
        );
        stubService.setUpdateResponse(response);

        mockMvc.perform(put("/api/v1/repositories/10/review-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failOnSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    void testUpdatePolicy_UnsupportedRuleId_Returns400BadRequest() throws Exception {
        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true, ReviewFindingSeverity.HIGH, Set.of("UNKNOWN-RULE-XYZ")
        );
        stubService.setException(new IllegalArgumentException("Unsupported rule ID 'UNKNOWN-RULE-XYZ'"));

        mockMvc.perform(put("/api/v1/repositories/10/review-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported rule ID 'UNKNOWN-RULE-XYZ'"));
    }

    @Test
    void testResetPolicy_ValidRepo_Returns200OK() throws Exception {
        RepositoryReviewPolicyResponse response = new RepositoryReviewPolicyResponse(
                1L, 10L, "my-repo", "owner/my-repo", true,
                ReviewFindingSeverity.HIGH,
                Set.of("RULE-JAVA-SYSTEM-OUT", "RULE-JAVA-EMPTY-CATCH", "RULE-TODO-FIXME"),
                List.of(), Instant.now(), Instant.now(), false
        );
        stubService.setResetResponse(response);

        mockMvc.perform(post("/api/v1/repositories/10/review-policy/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.failOnSeverity").value("HIGH"))
                .andExpect(jsonPath("$.isCustom").value(false));
    }

    // --- Stub Helper ---

    private static class StubRepositoryPolicyService extends RepositoryPolicyService {
        private RepositoryReviewPolicyResponse getResponse;
        private RepositoryReviewPolicyResponse updateResponse;
        private RepositoryReviewPolicyResponse resetResponse;
        private RuntimeException exception;

        public StubRepositoryPolicyService() {
            super(null, null, null, null);
        }

        void setGetResponse(RepositoryReviewPolicyResponse resp) { this.getResponse = resp; }
        void setUpdateResponse(RepositoryReviewPolicyResponse resp) { this.updateResponse = resp; }
        void setResetResponse(RepositoryReviewPolicyResponse resp) { this.resetResponse = resp; }
        void setException(RuntimeException ex) { this.exception = ex; }

        @Override
        public RepositoryReviewPolicyResponse getEffectivePolicy(Long repositoryId) {
            if (exception != null) throw exception;
            return getResponse;
        }

        @Override
        public RepositoryReviewPolicyResponse updatePolicy(Long repositoryId, RepositoryReviewPolicyRequest request) {
            if (exception != null) throw exception;
            return updateResponse;
        }

        @Override
        public RepositoryReviewPolicyResponse resetPolicy(Long repositoryId) {
            if (exception != null) throw exception;
            return resetResponse;
        }
    }
}
