package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeFixServiceTest {

    private CodeReviewFindingRepository findingRepository;
    private StubCurrentUserService currentUserService;
    private StubAiFixEngine stubAiFixEngine;
    private CodeFixService codeFixService;

    @BeforeEach
    void setUp() {
        findingRepository = mock(CodeReviewFindingRepository.class);
        currentUserService = new StubCurrentUserService();
        stubAiFixEngine = new StubAiFixEngine();
        codeFixService = new CodeFixService(findingRepository, currentUserService, null, stubAiFixEngine);
    }

    @Test
    void testGenerateFix_InvalidFindingId_ThrowsException() {
        assertThatThrownBy(() -> codeFixService.generateFix(null, new CodeFixRequest()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> codeFixService.generateFix(-1L, new CodeFixRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGenerateFix_FindingNotFound_ThrowsException() {
        when(findingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codeFixService.generateFix(99L, new CodeFixRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review finding not found with id: 99");
    }

    @Test
    void testGenerateFix_UnauthorizedUser_ThrowsAccessDeniedException() {
        User ownerUser = new User();
        ownerUser.setId(10L);

        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));
        currentUserService.setAuthenticated(true);
        currentUserService.setAdmin(false);
        currentUserService.setCurrentUserId(20L); // Different user ID

        assertThatThrownBy(() -> codeFixService.generateFix(1L, new CodeFixRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission");
    }

    @Test
    void testGenerateFix_AuthorizedUser_GeneratesFixSuccessfully() {
        User ownerUser = new User();
        ownerUser.setId(10L);

        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        CodeFixResponse expectedResponse = new CodeFixResponse(
                1L, "src/Test.java", "Explanation",
                "--- a/src/Test.java\n+++ b/src/Test.java\n@@ -12 +12 @@\n-old\n+new",
                "old", "new", Instant.now(), "Gemini", "PROPOSED", null
        );

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));
        currentUserService.setAuthenticated(true);
        currentUserService.setAdmin(false);
        currentUserService.setCurrentUserId(10L); // Same user ID
        stubAiFixEngine.setResponse(expectedResponse);

        CodeFixResponse result = codeFixService.generateFix(1L, new CodeFixRequest("Keep it simple"));

        assertThat(result).isNotNull();
        assertThat(result.getFindingId()).isEqualTo(1L);
        assertThat(result.getFilePath()).isEqualTo("src/Test.java");
        assertThat(result.getUnifiedDiff()).contains("+new");
        assertThat(result.getStatus()).isEqualTo("PROPOSED");
    }

    @Test
    void testGenerateFix_AdminUser_AccessAllowedRegardlessOfOwner() {
        User ownerUser = new User();
        ownerUser.setId(10L);

        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        CodeFixResponse expectedResponse = new CodeFixResponse(
                1L, "src/Test.java", "Explanation",
                "--- a/src/Test.java\n+++ b/src/Test.java\n@@ -12 +12 @@\n-old\n+new",
                "old", "new", Instant.now(), "Gemini", "PROPOSED", null
        );

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));
        currentUserService.setAuthenticated(true);
        currentUserService.setAdmin(true);
        stubAiFixEngine.setResponse(expectedResponse);

        CodeFixResponse result = codeFixService.generateFix(1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getFindingId()).isEqualTo(1L);
    }

    private static class StubCurrentUserService extends CurrentUserService {
        private boolean authenticated;
        private boolean admin;
        private Long currentUserId;

        public StubCurrentUserService() {
            super(null);
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public void setCurrentUserId(Long currentUserId) {
            this.currentUserId = currentUserId;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public boolean hasRole(String role) {
            return "ADMIN".equalsIgnoreCase(role) && admin;
        }

        @Override
        public Long getCurrentUserId() {
            return currentUserId;
        }
    }

    private static class StubAiFixEngine implements AiFixEngine {
        private CodeFixResponse response;
        private RuntimeException exception;

        public void setResponse(CodeFixResponse response) {
            this.response = response;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public CodeFixResponse generateFix(FixGenerationInput input) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
