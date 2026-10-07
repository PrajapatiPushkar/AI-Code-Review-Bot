package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeFixProposalResponse;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeFixServiceTest {

    private CodeReviewFindingRepository findingRepository;
    private StubCurrentUserService currentUserService;
    private StubAiFixEngine stubAiFixEngine;
    private InMemoryProposalRepository proposalRepository;
    private CodeFixService codeFixService;

    @BeforeEach
    void setUp() {
        findingRepository = mock(CodeReviewFindingRepository.class);
        currentUserService = new StubCurrentUserService();
        stubAiFixEngine = new StubAiFixEngine();
        proposalRepository = new InMemoryProposalRepository();
        codeFixService = new CodeFixService(
                findingRepository,
                currentUserService,
                null,
                stubAiFixEngine,
                proposalRepository,
                new GeminiProperties("test-key", "gemini-3.6-flash", "http://test")
        );
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
    void testGenerateFix_AuthorizedUser_GeneratesAndPersistsProposal() {
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
        currentUserService.setCurrentUserId(10L);
        stubAiFixEngine.setResponse(expectedResponse);

        CodeFixResponse result = codeFixService.generateFix(1L, new CodeFixRequest("Keep it simple"));

        assertThat(result).isNotNull();
        assertThat(result.getFindingId()).isEqualTo(1L);
        assertThat(result.getFilePath()).isEqualTo("src/Test.java");
        assertThat(result.getUnifiedDiff()).contains("+new");
        assertThat(result.getStatus()).isEqualTo("PROPOSED");
        assertThat(result.getProposalId()).isNotNull();

        // Verify persisted in repository
        Optional<CodeFixProposal> persisted = proposalRepository.findById(result.getProposalId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getFindingId()).isEqualTo(1L);
        assertThat(persisted.get().getFilePath()).isEqualTo("src/Test.java");
        assertThat(persisted.get().getStatus()).isEqualTo(CodeFixProposalStatus.PROPOSED);
        assertThat(persisted.get().getDeveloperInstructions()).isEqualTo("Keep it simple");
    }

    @Test
    void testGenerateFix_InstructionsExceeding500Chars_ThrowsIllegalArgumentException() {
        String longInstructions = "a".repeat(501);

        assertThatThrownBy(() -> codeFixService.generateFix(1L, new CodeFixRequest(longInstructions)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Developer instructions must not exceed 500 characters");
    }

    @Test
    void testGetFindingProposals_ReturnsNewestFirst() {
        User ownerUser = new User();
        ownerUser.setId(10L);
        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));
        currentUserService.setAuthenticated(true);
        currentUserService.setCurrentUserId(10L);

        CodeFixProposal p1 = new CodeFixProposal(1L, "src/Test.java", "Old", "diff1", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        p1.setCreatedAt(Instant.parse("2026-10-07T08:00:00Z"));
        CodeFixProposal p2 = new CodeFixProposal(1L, "src/Test.java", "Newer", "diff2", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        p2.setCreatedAt(Instant.parse("2026-10-07T09:00:00Z"));

        proposalRepository.save(p1);
        proposalRepository.save(p2);

        List<CodeFixProposalResponse> proposals = codeFixService.getFindingProposals(1L);

        assertThat(proposals).hasSize(2);
        assertThat(proposals.get(0).getExplanation()).isEqualTo("Newer");
        assertThat(proposals.get(1).getExplanation()).isEqualTo("Old");
    }

    @Test
    void testGetFindingProposals_UnauthorizedUser_ThrowsAccessDenied() {
        User ownerUser = new User();
        ownerUser.setId(10L);
        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));
        currentUserService.setAuthenticated(true);
        currentUserService.setCurrentUserId(99L); // Wrong user

        assertThatThrownBy(() -> codeFixService.getFindingProposals(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProposal_SuccessAndUnauthorized() {
        User ownerUser = new User();
        ownerUser.setId(10L);
        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);

        CodeFixProposal proposal = new CodeFixProposal(1L, "src/Test.java", "Exp", "diff", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        CodeFixProposal savedProposal = proposalRepository.save(proposal);

        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));

        // Authorized
        currentUserService.setAuthenticated(true);
        currentUserService.setCurrentUserId(10L);
        CodeFixProposalResponse resp = codeFixService.getProposal(savedProposal.getId());
        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(savedProposal.getId());

        // Unauthorized
        currentUserService.setCurrentUserId(999L);
        assertThatThrownBy(() -> codeFixService.getProposal(savedProposal.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testStatusTransitions() {
        User ownerUser = new User();
        ownerUser.setId(10L);
        CodeReview review = new CodeReview(12345L, "owner", "repo", 1, ownerUser);
        CodeReviewFinding finding = new CodeReviewFinding(review, "src/Test.java", 12, null,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug message", "Fix suggestion");
        finding.setId(1L);
        when(findingRepository.findById(1L)).thenReturn(Optional.of(finding));

        currentUserService.setAuthenticated(true);
        currentUserService.setCurrentUserId(10L);

        // PROPOSED -> REVIEWED
        CodeFixProposal p1 = new CodeFixProposal(1L, "src/Test.java", "Exp", "diff", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        CodeFixProposal savedP1 = proposalRepository.save(p1);
        CodeFixProposalResponse resp1 = codeFixService.updateProposalStatus(savedP1.getId(), CodeFixProposalStatus.REVIEWED);
        assertThat(resp1.getStatus()).isEqualTo(CodeFixProposalStatus.REVIEWED);

        // REVIEWED -> EXPIRED
        CodeFixProposalResponse resp2 = codeFixService.updateProposalStatus(savedP1.getId(), CodeFixProposalStatus.EXPIRED);
        assertThat(resp2.getStatus()).isEqualTo(CodeFixProposalStatus.EXPIRED);

        // EXPIRED -> PROPOSED (Invalid transition)
        assertThatThrownBy(() -> codeFixService.updateProposalStatus(savedP1.getId(), CodeFixProposalStatus.PROPOSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition from EXPIRED to PROPOSED");

        // PROPOSED -> REJECTED
        CodeFixProposal p2 = new CodeFixProposal(1L, "src/Test.java", "Exp", "diff", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        CodeFixProposal savedP2 = proposalRepository.save(p2);
        CodeFixProposalResponse respRejected = codeFixService.updateProposalStatus(savedP2.getId(), CodeFixProposalStatus.REJECTED);
        assertThat(respRejected.getStatus()).isEqualTo(CodeFixProposalStatus.REJECTED);

        // REJECTED -> REVIEWED (Invalid transition)
        assertThatThrownBy(() -> codeFixService.updateProposalStatus(savedP2.getId(), CodeFixProposalStatus.REVIEWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition from REJECTED to REVIEWED");

        // PROPOSED -> EXPIRED
        CodeFixProposal p3 = new CodeFixProposal(1L, "src/Test.java", "Exp", "diff", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        CodeFixProposal savedP3 = proposalRepository.save(p3);
        CodeFixProposalResponse respExpired = codeFixService.updateProposalStatus(savedP3.getId(), CodeFixProposalStatus.EXPIRED);
        assertThat(respExpired.getStatus()).isEqualTo(CodeFixProposalStatus.EXPIRED);
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

        public void setResponse(CodeFixResponse response) {
            this.response = response;
        }

        @Override
        public CodeFixResponse generateFix(FixGenerationInput input) {
            return response;
        }
    }

    private static class InMemoryProposalRepository implements CodeFixProposalRepository {
        private final List<CodeFixProposal> list = new ArrayList<>();
        private long idGen = 1L;

        @Override
        public List<CodeFixProposal> findByFindingIdOrderByCreatedAtDesc(Long findingId) {
            return list.stream()
                    .filter(p -> p.getFindingId().equals(findingId))
                    .sorted(Comparator.comparing(CodeFixProposal::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .toList();
        }

        @Override
        public Optional<CodeFixProposal> findById(Long id) {
            return list.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public <S extends CodeFixProposal> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            list.removeIf(p -> p.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public <S extends CodeFixProposal> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(Long id) { return list.stream().anyMatch(p -> p.getId().equals(id)); }
        @Override public List<CodeFixProposal> findAll() { return new ArrayList<>(list); }
        @Override public List<CodeFixProposal> findAllById(Iterable<Long> ids) { return null; }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long id) { list.removeIf(p -> p.getId().equals(id)); }
        @Override public void delete(CodeFixProposal entity) { list.remove(entity); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { }
        @Override public void deleteAll(Iterable<? extends CodeFixProposal> entities) { }
        @Override public void deleteAll() { list.clear(); }
        @Override public void flush() { }
        @Override public <S extends CodeFixProposal> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeFixProposal> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<CodeFixProposal> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public CodeFixProposal getOne(Long id) { return null; }
        @Override public CodeFixProposal getById(Long id) { return null; }
        @Override public CodeFixProposal getReferenceById(Long id) { return null; }
        @Override public <S extends CodeFixProposal> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends CodeFixProposal> List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends CodeFixProposal> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override public <S extends CodeFixProposal> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends CodeFixProposal> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends CodeFixProposal> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends CodeFixProposal, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<CodeFixProposal> findAll(org.springframework.data.domain.Sort sort) { return null; }
        @Override public org.springframework.data.domain.Page<CodeFixProposal> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}
