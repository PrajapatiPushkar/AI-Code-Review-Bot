package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.ai.AiReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubPullRequestCodeReviewServiceTest {

    private static final Long INSTALLATION_ID = 12345L;
    private static final String OWNER = "octocat";
    private static final String REPO = "hello-world";
    private static final int PR_NUMBER = 42;
    private static final String COMMIT_SHA = "sha999888";

    private StubPullRequestReviewService pullRequestReviewService;
    private StubAiReviewService aiReviewService;
    private StubReviewCommentService reviewCommentService;
    private StubPersistenceService persistenceService;
    private StubCurrentUserService currentUserService;
    private StubGithubInstallationRepository installationRepository;
    private GithubPullRequestCodeReviewService codeReviewService;

    private User user1;
    private User user2;
    private User devUser;
    private User adminUser;
    private ReviewInput sampleInput;

    @BeforeEach
    void setUp() {
        pullRequestReviewService = new StubPullRequestReviewService();
        aiReviewService = new StubAiReviewService();
        reviewCommentService = new StubReviewCommentService();
        persistenceService = new StubPersistenceService();
        currentUserService = new StubCurrentUserService();
        installationRepository = new StubGithubInstallationRepository();

        codeReviewService = new GithubPullRequestCodeReviewService(
                pullRequestReviewService, aiReviewService, reviewCommentService, persistenceService, currentUserService, installationRepository
        );

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);

        devUser = new User("devuser", "dev@example.com", "hash", "DEVELOPER");
        devUser.setId(30L);

        adminUser = new User("adminuser", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);

        sampleInput = new ReviewInput(
                100L, REPO, OWNER + "/" + REPO, "https://github.com/octocat/hello-world", "main",
                200L, (long) PR_NUMBER, "PR Title", "PR Body", "open",
                "https://github.com/octocat/hello-world/pull/42", OWNER, COMMIT_SHA, "main",
                Instant.now(), Instant.now(), List.of(new ReviewFileInput("Main.java", "modified", 5, 1, 6, "@@ -1 +1 @@", null))
        );

        GithubInstallation verifiedInstallation = new GithubInstallation(user1, INSTALLATION_ID, OWNER, "User");
        verifiedInstallation.setId(1L);
        verifiedInstallation.setVerified(true);
        verifiedInstallation.setVerifiedAt(Instant.now());
        installationRepository.save(verifiedInstallation);
    }

    @Test
    void testExecuteCodeReview_SuccessFlowWithPersistence_UserRole() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);

        ReviewFinding finding1 = new ReviewFinding("Main.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        ReviewFinding finding2 = new ReviewFinding("Main.java", 20, ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.PERFORMANCE, "Perf msg", "Fix perf");
        ReviewResult sampleResult = new ReviewResult("Review summary text", List.of(finding1, finding2));

        GithubReviewCommentResponse comment1 = new GithubReviewCommentResponse(1L, "body1", "Main.java", 10, COMMIT_SHA, "url1", Instant.now());
        GithubReviewCommentResponse comment2 = new GithubReviewCommentResponse(2L, "body2", "Main.java", 20, COMMIT_SHA, "url2", Instant.now());

        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(sampleResult);
        reviewCommentService.setCommentResponses(List.of(comment1, comment2));

        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(1L, OWNER, REPO, PR_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getCodeReviewId()).isEqualTo(100L);
        assertThat(result.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(result.getOwner()).isEqualTo(OWNER);
        assertThat(result.getRepository()).isEqualTo(REPO);
        assertThat(result.getPullRequestNumber()).isEqualTo((long) PR_NUMBER);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getReviewSummary()).isEqualTo("Review summary text");
        assertThat(result.getTotalFindings()).isEqualTo(2);
        assertThat(result.getPostedCommentsCount()).isEqualTo(2);

        // Persistence verifications
        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedCompleted()).isTrue();
        assertThat(persistenceService.getLastEntity().getUser()).isEqualTo(user1);
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
    }

    @Test
    void testExecuteCodeReview_DeveloperRole_Success() {
        GithubInstallation devInst = new GithubInstallation(devUser, 55555L, "devowner", "User");
        devInst.setId(2L);
        devInst.setVerified(true);
        installationRepository.save(devInst);

        currentUserService.setContext(devUser.getId(), "dev@example.com", "DEVELOPER", devUser);
        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(new ReviewResult("Dev review summary", Collections.emptyList()));
        reviewCommentService.setCommentResponses(Collections.emptyList());

        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(2L, "devowner", REPO, PR_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(persistenceService.getLastEntity().getUser()).isEqualTo(devUser);
    }

    @Test
    void testExecuteCodeReview_AdminRole_CanReviewAnotherUserInstallation() {
        currentUserService.setContext(adminUser.getId(), "admin@example.com", "ADMIN", adminUser);
        pullRequestReviewService.setReviewInput(sampleInput);
        aiReviewService.setReviewResult(new ReviewResult("Admin review summary", Collections.emptyList()));

        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(1L, OWNER, REPO, PR_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void testExecuteCodeReview_UserCannotUseAnotherUserInstallation_ThrowsAccessDenied() {
        currentUserService.setContext(user2.getId(), "user2@example.com", "USER", user2);

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(1L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void testExecuteCodeReview_UnverifiedInstallation_ThrowsGithubInstallationVerificationException() {
        GithubInstallation unverified = new GithubInstallation(user1, 9999L, "unverifiedowner", "User");
        unverified.setId(3L);
        unverified.setVerified(false);
        installationRepository.save(unverified);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(3L, "unverifiedowner", REPO, PR_NUMBER))
                .isInstanceOf(GithubInstallationVerificationException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void testExecuteCodeReview_MissingInstallation_ThrowsResourceNotFoundException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(999L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testExecuteCodeReview_GitHubRetrievalFailureMarksFailed() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);
        pullRequestReviewService.setException(new ResourceNotFoundException("GitHub PR not found"));

        assertThatThrownBy(() -> codeReviewService.executeCodeReview(1L, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GitHub PR not found");

        assertThat(persistenceService.isCreatedInProgress()).isTrue();
        assertThat(persistenceService.isMarkedFailed()).isTrue();
        assertThat(persistenceService.getLastEntity().getStatus()).isEqualTo(CodeReviewStatus.FAILED);
    }

    @Test
    void testExecuteCodeReview_InvalidParameters() {
        assertThatThrownBy(() -> codeReviewService.executeCodeReview(null, OWNER, REPO, PR_NUMBER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(persistenceService.isCreatedInProgress()).isFalse();
    }

    // --- Helper Stubs ---

    private static class StubPullRequestReviewService extends GithubPullRequestReviewService {
        private ReviewInput reviewInput;
        private RuntimeException exception;

        public StubPullRequestReviewService() { super(null, null, null, null); }

        public void setReviewInput(ReviewInput reviewInput) { this.reviewInput = reviewInput; }
        public void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public ReviewInput getReviewInput(Long installationId, String owner, String repository, long pullRequestNumber) {
            if (exception != null) throw exception;
            return reviewInput;
        }
    }

    private static class StubAiReviewService extends AiReviewService {
        private ReviewResult reviewResult;
        private RuntimeException exception;

        public StubAiReviewService() { super(null); }

        public void setReviewResult(ReviewResult reviewResult) { this.reviewResult = reviewResult; }
        public void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public ReviewResult review(ReviewInput input) {
            if (exception != null) throw exception;
            return reviewResult;
        }
    }

    private static class StubReviewCommentService extends GithubReviewCommentService {
        private List<GithubReviewCommentResponse> commentResponses = new ArrayList<>();
        private RuntimeException exception;

        public StubReviewCommentService() { super(null); }

        public void setCommentResponses(List<GithubReviewCommentResponse> commentResponses) { this.commentResponses = commentResponses; }
        public void setException(RuntimeException exception) { this.exception = exception; }

        @Override
        public List<GithubReviewCommentResponse> postReviewComments(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
            if (exception != null) throw exception;
            return commentResponses;
        }
    }

    private static class StubPersistenceService extends CodeReviewPersistenceService {
        private CodeReview entity;
        private boolean createdInProgress = false;
        private boolean markedCompleted = false;
        private boolean markedFailed = false;

        public StubPersistenceService() { super(null); }

        public boolean isCreatedInProgress() { return createdInProgress; }
        public boolean isMarkedCompleted() { return markedCompleted; }
        public boolean isMarkedFailed() { return markedFailed; }
        public CodeReview getLastEntity() { return entity; }

        @Override
        public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber, User user) {
            this.createdInProgress = true;
            this.entity = new CodeReview(installationId, owner, repositoryName, pullRequestNumber, user);
            this.entity.setId(100L);
            this.entity.setStatus(CodeReviewStatus.IN_PROGRESS);
            this.entity.setCreatedAt(Instant.now());
            return entity;
        }

        @Override
        public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
            this.markedCompleted = true;
            if (entity != null) {
                entity.setStatus(CodeReviewStatus.COMPLETED);
                entity.setReviewSummary(reviewSummary);
                entity.setTotalFindings(totalFindings);
                entity.setPostedCommentsCount(postedCommentsCount);
                entity.setCompletedAt(Instant.now());
            }
            return entity;
        }

        @Override
        public CodeReview markFailed(Long reviewId, String errorMessage) {
            this.markedFailed = true;
            if (entity != null) {
                entity.setStatus(CodeReviewStatus.FAILED);
                entity.setReviewSummary("FAILED: " + errorMessage);
                entity.setCompletedAt(Instant.now());
            }
            return entity;
        }
    }

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;
        private User user;

        public StubCurrentUserService() { super(null); }

        public void setContext(Long userId, String username, String role, User user) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.user = user;
        }

        @Override public boolean isAuthenticated() { return username != null; }
        @Override public String getCurrentUsername() { return username; }
        @Override public Long getCurrentUserId() { return userId; }
        @Override public User getCurrentUser() { return user; }
        @Override
        public boolean hasRole(String roleName) {
            if (role == null) return false;
            String normalized = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            return role.equalsIgnoreCase(normalized);
        }
    }

    private static class StubGithubInstallationRepository extends StubJpaRepository<GithubInstallation, Long> implements GithubInstallationRepository {
        @Override
        public boolean existsByGithubInstallationId(Long githubInstallationId) {
            return database.values().stream().anyMatch(i -> githubInstallationId != null && githubInstallationId.equals(i.getGithubInstallationId()));
        }

        @Override
        public List<GithubInstallation> findByUserId(Long userId) {
            return database.values().stream().filter(i -> i.getUser() != null && userId.equals(i.getUser().getId())).toList();
        }

        @Override
        public Optional<GithubInstallation> findByGithubInstallationId(Long githubInstallationId) {
            return database.values().stream().filter(i -> githubInstallationId != null && githubInstallationId.equals(i.getGithubInstallationId())).findFirst();
        }

        @Override
        public <S extends GithubInstallation> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId((long) (database.size() + 1));
            }
            database.put(entity.getId(), entity);
            return entity;
        }
    }

    private static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID> {
        protected final Map<ID, T> database = new HashMap<>();

        @Override public Optional<T> findById(ID id) { return Optional.ofNullable(database.get(id)); }
        @Override public List<T> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<T> findAllById(Iterable<ID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(ID id) { database.remove(id); }
        @Override public void delete(T entity) { }
        @Override public void deleteAllById(Iterable<? extends ID> ids) { }
        @Override public void deleteAll(Iterable<? extends T> entities) { }
        @Override public void deleteAll() { database.clear(); }
        @Override public <S extends T> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(ID id) { return database.containsKey(id); }
        @Override public void flush() { }
        @Override public <S extends T> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<T> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<ID> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public T getOne(ID id) { return database.get(id); }
        @Override public T getById(ID id) { return database.get(id); }
        @Override public T getReferenceById(ID id) { return database.get(id); }
        @Override public List<T> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
