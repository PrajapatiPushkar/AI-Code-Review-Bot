package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewFindingResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewResultResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewStatusResponse;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewSpecification;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private StubCodeReviewFindingRepository findingRepository;
    private StubCurrentUserService currentUserService;
    private CodeReviewHistoryService service;

    private User user1;
    private User user2;
    private User devUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        repository = new StubCodeReviewRepository();
        findingRepository = new StubCodeReviewFindingRepository();
        currentUserService = new StubCurrentUserService();
        service = new CodeReviewHistoryService(repository, currentUserService, findingRepository);

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);

        devUser = new User("devuser", "dev@example.com", "hash", "DEVELOPER");
        devUser.setId(30L);

        adminUser = new User("adminuser", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);
    }

    @AfterEach
    void tearDown() {
        currentUserService.clear();
    }

    @Test
    void testGetResultById_Completed_ReturnsFindings() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(devUser);
        repository.save(review);

        CodeReviewFinding f1 = new CodeReviewFinding(review, "App.java", 10, 12, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        f1.setId(101L);
        findingRepository.save(f1);

        currentUserService.setContext(devUser.getId(), "dev@example.com", "DEVELOPER");

        CodeReviewResultResponse response = service.getResultById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getCodeReviewId()).isEqualTo(1L);
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getFilePath()).isEqualTo("App.java");
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void testGetFindingsByReviewId_UserGetsOwnFindings() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        CodeReviewFinding f1 = new CodeReviewFinding(review, "App.java", 10, 12, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        f1.setId(101L);
        findingRepository.save(f1);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        Page<CodeReviewFindingResponse> page = service.getFindingsByReviewId(1L, 0, 20, "lineNumber,asc");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getFilePath()).isEqualTo("App.java");
    }

    @Test
    void testGetFindingsByReviewId_UserCannotAccessAnotherUsersFindings_ThrowsAccessDenied() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        currentUserService.setContext(user2.getId(), "user2@example.com", "USER");

        assertThatThrownBy(() -> service.getFindingsByReviewId(1L, 0, 20, "lineNumber,asc"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void testGetFindingsByReviewId_AdminCanAccessAnyFindings() {
        CodeReview review = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        review.setUser(user1);
        repository.save(review);

        CodeReviewFinding f1 = new CodeReviewFinding(review, "App.java", 10, 12, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug msg", "Fix bug");
        f1.setId(101L);
        findingRepository.save(f1);

        currentUserService.setContext(adminUser.getId(), "admin@example.com", "ADMIN");

        Page<CodeReviewFindingResponse> page = service.getFindingsByReviewId(1L, 0, 20, "lineNumber,asc");

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void testGetFindingsByReviewId_NotFound_ThrowsResourceNotFoundException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        assertThatThrownBy(() -> service.getFindingsByReviewId(999L, 0, 20, "lineNumber,asc"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testGetFindingsByReviewId_InvalidPageOrSize_ThrowsIllegalArgumentException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        assertThatThrownBy(() -> service.getFindingsByReviewId(1L, -1, 20, "lineNumber,asc"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.getFindingsByReviewId(1L, 0, 101, "lineNumber,asc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CodeReview createSampleReview(Long id, Long installationId, String owner, String repo, int prNumber, CodeReviewStatus status) {
        CodeReview review = new CodeReview(installationId, owner, repo, prNumber);
        review.setId(id);
        review.setReviewSummary(status == CodeReviewStatus.IN_PROGRESS ? "" : "Found 2 potential issues.");
        review.setTotalFindings(status == CodeReviewStatus.IN_PROGRESS ? 0 : 2);
        review.setPostedCommentsCount(status == CodeReviewStatus.IN_PROGRESS ? 0 : 2);
        review.setStatus(status);
        review.setCreatedAt(Instant.parse("2026-08-16T05:00:00Z"));
        review.setCompletedAt(status == CodeReviewStatus.IN_PROGRESS ? null : Instant.parse("2026-08-16T05:00:12Z"));
        return review;
    }

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;

        public StubCurrentUserService() { super(null); }

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

        @Override public boolean isAuthenticated() { return username != null; }
        @Override public String getCurrentUsername() { return username; }
        @Override public Long getCurrentUserId() { return userId; }
        @Override
        public boolean hasRole(String roleName) {
            if (this.role == null) return false;
            String normalized = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            return this.role.equalsIgnoreCase(normalized);
        }
    }

    private static class StubCodeReviewFindingRepository extends StubJpaRepository<CodeReviewFinding, Long> implements CodeReviewFindingRepository {
        @Override
        public List<CodeReviewFinding> findByCodeReviewIdOrderByFilePathAscLineNumberAsc(Long codeReviewId) {
            return database.values().stream()
                    .filter(f -> f.getCodeReview() != null && codeReviewId.equals(f.getCodeReview().getId()))
                    .sorted(Comparator.comparing(CodeReviewFinding::getFilePath).thenComparing(CodeReviewFinding::getLineNumber))
                    .toList();
        }

        @Override
        public Page<CodeReviewFinding> findByCodeReviewId(Long codeReviewId, Pageable pageable) {
            List<CodeReviewFinding> list = database.values().stream()
                    .filter(f -> f.getCodeReview() != null && codeReviewId.equals(f.getCodeReview().getId()))
                    .toList();
            return new PageImpl<>(list, pageable, list.size());
        }

        @Override
        public <S extends CodeReviewFinding> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId((long) (database.size() + 1));
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public <S extends CodeReviewFinding> List<S> saveAll(Iterable<S> entities) {
            List<S> result = new ArrayList<>();
            for (S entity : entities) {
                result.add(save(entity));
            }
            return result;
        }
    }

    private static class StubCodeReviewRepository extends StubJpaRepository<CodeReview, Long> implements CodeReviewRepository {
        @Override public <S extends CodeReview> S save(S entity) { database.put(entity.getId(), entity); return entity; }

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

        @Override public List<CodeReview> findAllByOrderByCreatedAtDesc() { return database.values().stream().sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed()).toList(); }

        @Override
        public List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId) {
            return database.values().stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getId())).sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed()).toList();
        }

        @Override
        public List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository) {
            return database.values().stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()) && r.getOwner().equals(owner) && r.getRepository().equals(repository)).sorted(Comparator.comparing(CodeReview::getCreatedAt).reversed()).toList();
        }

        @Override
        public List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber) {
            return database.values().stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()) && r.getOwner().equals(owner) && r.getRepository().equals(repository) && r.getPullRequestNumber().equals(pullRequestNumber)).toList();
        }

        @Override
        public Optional<CodeReview> findByIdAndUserId(Long id, Long userId) {
            return database.values().stream().filter(r -> r.getId().equals(id) && r.getUser() != null && userId.equals(r.getUser().getId())).findFirst();
        }
    }

    private static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<T> {
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
        @Override public Page<T> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> Page<S> findAll(org.springframework.data.domain.Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }

        @Override public Optional<T> findOne(Specification<T> spec) { return Optional.empty(); }
        @Override public List<T> findAll(Specification<T> spec) { return new ArrayList<>(database.values()); }
        @SuppressWarnings("unchecked")
        @Override public Page<T> findAll(Specification<T> spec, Pageable pageable) {
            List<T> list = database.values().stream().filter(item -> {
                if (item instanceof CodeReview r && spec instanceof CodeReviewSpecification s) {
                    if (s.getUserId() != null && (r.getUser() == null || !s.getUserId().equals(r.getUser().getId()))) return false;
                    if (s.getStatus() != null && r.getStatus() != s.getStatus()) return false;
                    if (s.getOwner() != null && !s.getOwner().isBlank() && !r.getOwner().equalsIgnoreCase(s.getOwner())) return false;
                    if (s.getRepository() != null && !s.getRepository().isBlank() && !r.getRepository().equalsIgnoreCase(s.getRepository())) return false;
                    if (s.getPullRequestNumber() != null && !s.getPullRequestNumber().equals(r.getPullRequestNumber())) return false;
                }
                return true;
            }).toList();
            return new PageImpl<>(list, pageable, list.size());
        }
        @Override public List<T> findAll(Specification<T> spec, org.springframework.data.domain.Sort sort) { return new ArrayList<>(database.values()); }
        @Override public long count(Specification<T> spec) { return database.size(); }
        @Override public boolean exists(Specification<T> spec) { return !database.isEmpty(); }
        @Override public long delete(Specification<T> spec) { return 0; }
        @Override public <S extends T, R> R findBy(Specification<T> spec, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
