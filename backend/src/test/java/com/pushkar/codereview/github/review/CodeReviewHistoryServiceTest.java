package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private CodeReviewHistoryService service;

    @BeforeEach
    void setUp() {
        repository = new StubCodeReviewRepository();
        service = new CodeReviewHistoryService(repository);
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
        assertThat(response.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(response.getCompletedAt()).isEqualTo(entity.getCompletedAt());
    }

    @Test
    void testGetById_MissingReview_ThrowsResourceNotFoundException() {
        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CodeReview record not found with id: 999");
    }

    @Test
    void testGetByRepository_Success() {
        CodeReview r1 = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        CodeReview r2 = createSampleReview(2L, 123456L, "octocat", "hello-world", 43, CodeReviewStatus.IN_PROGRESS);
        repository.save(r1);
        repository.save(r2);

        List<CodeReviewHistoryResponse> responses = service.getByRepository("octocat", "hello-world");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CodeReviewHistoryResponse::getOwner).containsOnly("octocat");
        assertThat(responses).extracting(CodeReviewHistoryResponse::getRepository).containsOnly("hello-world");
    }

    @Test
    void testGetByPullRequest_Success() {
        CodeReview r1 = createSampleReview(1L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.COMPLETED);
        CodeReview r2 = createSampleReview(2L, 123456L, "octocat", "hello-world", 42, CodeReviewStatus.FAILED);
        CodeReview r3 = createSampleReview(3L, 123456L, "octocat", "hello-world", 99, CodeReviewStatus.COMPLETED);
        repository.save(r1);
        repository.save(r2);
        repository.save(r3);

        List<CodeReviewHistoryResponse> responses = service.getByPullRequest("octocat", "hello-world", 42);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CodeReviewHistoryResponse::getPullRequestNumber).containsOnly(42);
    }

    @Test
    void testInvalidId_Rejected() {
        assertThatThrownBy(() -> service.getById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review ID must be positive");

        assertThatThrownBy(() -> service.getById(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review ID must be positive");

        assertThatThrownBy(() -> service.getById(-5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review ID must be positive");
    }

    @Test
    void testBlankOwner_Rejected() {
        assertThatThrownBy(() -> service.getByRepository(null, "hello-world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner must not be blank");

        assertThatThrownBy(() -> service.getByRepository("   ", "hello-world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner must not be blank");

        assertThatThrownBy(() -> service.getByPullRequest("", "hello-world", 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner must not be blank");
    }

    @Test
    void testBlankRepository_Rejected() {
        assertThatThrownBy(() -> service.getByRepository("octocat", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository must not be blank");

        assertThatThrownBy(() -> service.getByRepository("octocat", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository must not be blank");

        assertThatThrownBy(() -> service.getByPullRequest("octocat", "", 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository must not be blank");
    }

    @Test
    void testInvalidPullRequestNumber_Rejected() {
        assertThatThrownBy(() -> service.getByPullRequest("octocat", "hello-world", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be positive");

        assertThatThrownBy(() -> service.getByPullRequest("octocat", "hello-world", -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pull request number must be positive");
    }

    @Test
    void testEntityToDtoMapping_TimestampsAndStatusPreserved() {
        Instant now = Instant.now();
        Instant completedAt = now.plusSeconds(12);

        CodeReview entity = new CodeReview(123456L, "octocat", "hello-world", 42);
        entity.setId(100L);
        entity.setReviewSummary("Passed cleanly.");
        entity.setTotalFindings(5);
        entity.setPostedCommentsCount(3);
        entity.setStatus(CodeReviewStatus.FAILED);
        entity.setCreatedAt(now);
        entity.setCompletedAt(completedAt);

        repository.save(entity);

        CodeReviewHistoryResponse dto = service.getById(100L);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getInstallationId()).isEqualTo(123456L);
        assertThat(dto.getOwner()).isEqualTo("octocat");
        assertThat(dto.getRepository()).isEqualTo("hello-world");
        assertThat(dto.getPullRequestNumber()).isEqualTo(42);
        assertThat(dto.getReviewSummary()).isEqualTo("Passed cleanly.");
        assertThat(dto.getTotalFindings()).isEqualTo(5);
        assertThat(dto.getPostedCommentsCount()).isEqualTo(3);
        assertThat(dto.getStatus()).isEqualTo(CodeReviewStatus.FAILED);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getCompletedAt()).isEqualTo(completedAt);
    }

    // --- Helper & Stub ---

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
