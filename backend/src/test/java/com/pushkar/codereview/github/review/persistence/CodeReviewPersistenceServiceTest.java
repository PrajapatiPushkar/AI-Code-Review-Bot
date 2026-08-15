package com.pushkar.codereview.github.review.persistence;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeReviewPersistenceServiceTest {

    private StubCodeReviewRepository stubRepository;
    private CodeReviewPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        stubRepository = new StubCodeReviewRepository();
        persistenceService = new CodeReviewPersistenceService(stubRepository);
    }

    @Test
    void testCreateInProgressReview() {
        CodeReview review = persistenceService.createInProgressReview(12345L, "octocat", "hello-world", 42);

        assertThat(review).isNotNull();
        assertThat(review.getId()).isEqualTo(1L);
        assertThat(review.getInstallationId()).isEqualTo(12345L);
        assertThat(review.getOwner()).isEqualTo("octocat");
        assertThat(review.getRepository()).isEqualTo("hello-world");
        assertThat(review.getPullRequestNumber()).isEqualTo(42);
        assertThat(review.getStatus()).isEqualTo(CodeReviewStatus.IN_PROGRESS);
        assertThat(review.getCreatedAt()).isNotNull();
        assertThat(review.getCompletedAt()).isNull();
    }

    @Test
    void testMarkCompleted() {
        CodeReview inProgress = persistenceService.createInProgressReview(12345L, "octocat", "hello-world", 42);
        Instant startTime = inProgress.getCreatedAt();

        CodeReview completed = persistenceService.markCompleted(inProgress.getId(), "Review passed", 3, 2);

        assertThat(completed.getId()).isEqualTo(inProgress.getId());
        assertThat(completed.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
        assertThat(completed.getReviewSummary()).isEqualTo("Review passed");
        assertThat(completed.getTotalFindings()).isEqualTo(3);
        assertThat(completed.getPostedCommentsCount()).isEqualTo(2);
        assertThat(completed.getCreatedAt()).isEqualTo(startTime);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getCompletedAt()).isAfterOrEqualTo(startTime);
    }

    @Test
    void testMarkFailed() {
        CodeReview inProgress = persistenceService.createInProgressReview(12345L, "octocat", "hello-world", 42);

        CodeReview failed = persistenceService.markFailed(inProgress.getId(), "API Rate Limit Exceeded");

        assertThat(failed.getId()).isEqualTo(inProgress.getId());
        assertThat(failed.getStatus()).isEqualTo(CodeReviewStatus.FAILED);
        assertThat(failed.getReviewSummary()).contains("FAILED: API Rate Limit Exceeded");
        assertThat(failed.getCompletedAt()).isNotNull();
    }

    @Test
    void testMarkCompleted_NotFound() {
        assertThatThrownBy(() -> persistenceService.markCompleted(999L, "Summary", 1, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CodeReview record not found with id: 999");
    }

    // --- Stub Repository ---

    private static class StubCodeReviewRepository extends StubJpaRepository<CodeReview, Long> implements CodeReviewRepository {
        private long idSequence = 1L;

        @Override
        public <S extends CodeReview> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idSequence++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public java.util.List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber) {
            return database.values().stream()
                    .filter(r -> r.getOwner().equals(owner) && r.getRepository().equals(repository) && r.getPullRequestNumber().equals(pullRequestNumber))
                    .toList();
        }

        @Override
        public java.util.List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository) {
            return database.values().stream()
                    .filter(r -> r.getOwner().equals(owner) && r.getRepository().equals(repository))
                    .sorted(java.util.Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public java.util.List<CodeReview> findAllByOrderByCreatedAtDesc() {
            return database.values().stream()
                    .sorted(java.util.Comparator.comparing(CodeReview::getCreatedAt).reversed())
                    .toList();
        }
    }

    private static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID> {
        protected final java.util.Map<ID, T> database = new java.util.HashMap<>();

        @Override
        public java.util.Optional<T> findById(ID id) {
            return java.util.Optional.ofNullable(database.get(id));
        }

        @Override public java.util.List<T> findAll() { return new java.util.ArrayList<>(database.values()); }
        @Override public java.util.List<T> findAllById(Iterable<ID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(ID id) { database.remove(id); }
        @Override public void delete(T entity) { }
        @Override public void deleteAllById(Iterable<? extends ID> ids) { }
        @Override public void deleteAll(Iterable<? extends T> entities) { }
        @Override public void deleteAll() { database.clear(); }
        @Override public <S extends T> java.util.List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(ID id) { return database.containsKey(id); }
        @Override public void flush() { }
        @Override public <S extends T> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends T> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<T> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<ID> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public T getOne(ID id) { return database.get(id); }
        @Override public T getById(ID id) { return database.get(id); }
        @Override public T getReferenceById(ID id) { return database.get(id); }
        @Override public <S extends T> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
