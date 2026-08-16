package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeReviewHistoryServiceTest {

    private StubCodeReviewRepository repository;
    private StubCurrentUserService currentUserService;
    private CodeReviewHistoryService service;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        repository = new StubCodeReviewRepository();
        currentUserService = new StubCurrentUserService();
        service = new CodeReviewHistoryService(repository, currentUserService);

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);
    }

    @AfterEach
    void tearDown() {
        currentUserService.clear();
    }

    @Test
    void testGetById_Success() {
        CodeReview entity = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        repository.save(entity);

        CodeReviewHistoryResponse response = service.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getInstallationId()).isEqualTo(123456L);
        assertThat(response.getOwner()).isEqualTo("octocat");
        assertThat(response.getRepository()).isEqualTo("hello-world");
        assertThat(response.getPullRequestNumber()).isEqualTo(42);
        assertThat(response.getReviewSummary()).isEqualTo("Found 2 potential issues.");
        assertThat(response.getTotalFindings()).isEqualTo(2);
        assertThat(response.getPostedCommentsCount()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
    }

    @Test
    void testGetById_UserCanAccessOwnReview() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        CodeReviewHistoryResponse response = service.getById(1L);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void testGetById_UserCannotAccessAnotherUsersReview_ThrowsAccessDeniedException() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        currentUserService.setContext(user2.getId(), "user2@example.com", "USER");

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to access this code review");
    }

    @Test
    void testGetById_AdminCanAccessAnotherUsersReview() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        currentUserService.setContext(99L, "admin@example.com", "ADMIN");

        CodeReviewHistoryResponse response = service.getById(1L);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void testGetByRepository_UserSeesOnlyOwnReviews() {
        CodeReview r1 = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        r1.setUser(user1);
        CodeReview r2 = createSampleReview(2L, 123456L, "octocat", "hello-world", 43, CodeReviewStatus.COMPLETED);
        r2.setUser(user2);
        repository.save(r1);
        repository.save(r2);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        List<CodeReviewHistoryResponse> responses = service.getByRepository("octocat", "hello-world");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void testGetByRepository_AdminSeesAllReviews() {
        CodeReview r1 = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        r1.setUser(user1);
        CodeReview r2 = createSampleReview(2L, 123456L, "octocat", "hello-world", 43, CodeReviewStatus.COMPLETED);
        r2.setUser(user2);
        repository.save(r1);
        repository.save(r2);

        currentUserService.setContext(99L, "admin@example.com", "ADMIN");

        List<CodeReviewHistoryResponse> responses = service.getByRepository("octocat", "hello-world");

        assertThat(responses).hasSize(2);
    }

    @Test
    void testGetById_MissingReview_ThrowsResourceNotFoundException() {
        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CodeReview record not found with id: 999");
    }

    @Test
    void testInvalidId_Rejected() {
        assertThatThrownBy(() -> service.getById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review ID must be positive");

        assertThatThrownBy(() -> service.getById(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review ID must be positive");
    }

    private CodeReview createSampleReview(Long id, Long installationId, String owner, String repo, int prNumber, CodeReviewStatus status) {
        CodeReview review = new CodeReview(installationId, owner, repo, prNumber);
        review.setId(id);
        review.setReviewSummary("Found 2 potential issues.");
        review.setTotalFindings(2);
        review.setPostedCommentsCount(2);
        review.setStatus(status);
        review.setCreatedAt(Instant.parse("2026-08-16T05:00:00Z"));
        review.setCompletedAt(status == CodeReviewStatus.IN_PROGRESS ? null : Instant.parse("2026-08-16T05:00:12Z"));
        return review;
    }

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;

        public StubCurrentUserService() {
            super(null);
        }

        public void setContext(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public void clear() {
            this.userId = null;
            this.username = null;
            this.role = null;
        }

        @Override
        public boolean isAuthenticated() {
            return username != null;
        }

        @Override
        public String getCurrentUsername() {
            return username;
        }

        @Override
        public Long getCurrentUserId() {
            return userId;
        }

        @Override
        public boolean hasRole(String roleName) {
            if (this.role == null) return false;
            String normalized = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            return this.role.equalsIgnoreCase(normalized);
        }
    }

    private static class StubCodeReviewRepository extends StubJpaRepository<CodeReview, Long> implements CodeReviewRepository {
        @Override
        public <S extends CodeReview> S save(S entity) {
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber) {
            return database.values().stream()
                    .filter(r -> r.getOwner().equals(owner) && r.getRepository().equals(repository) && r.getPullRequestNumber().equals(pullRequestNumber))
                    .toList();
        }

        @Override
        public List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository) {
            return database.values().stream()
                    .filter(r -> r.getOwner().equals(owner) && r.getRepository().equals(repository))
                    .sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public List<CodeReview> findAllByOrderByCreatedAtDesc() {
            return database.values().stream()
                    .sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId) {
            return database.values().stream()
                    .filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()))
                    .sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository) {
            return database.values().stream()
                    .filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()) && r.getOwner().equals(owner) && r.getRepository().equals(repository))
                    .sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber) {
            return database.values().stream()
                    .filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()) && r.getOwner().equals(owner) && r.getRepository().equals(repository) && r.getPullRequestNumber().equals(pullRequestNumber))
                    .toList();
        }

        @Override
        public Optional<CodeReview> findByIdAndUserId(Long id, Long userId) {
            return database.values().stream()
                    .filter(r -> r.getId().equals(id) && r.getUser() != null && userId.equals(r.getUser().getId()))
                    .findFirst();
        }
    }

    private static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID> {
        protected final Map<ID, T> database = new HashMap<>();

        @Override
        public Optional<T> findById(ID id) {
            return Optional.ofNullable(database.get(id));
        }

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
