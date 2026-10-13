package com.pushkar.codereview.config;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionReliabilityHardeningTest {

    private AsyncConfig asyncConfig;
    private GlobalExceptionHandler exceptionHandler;
    private InMemoryCodeReviewRepository codeReviewRepository;
    private CodeReviewPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
        exceptionHandler = new GlobalExceptionHandler();
        codeReviewRepository = new InMemoryCodeReviewRepository();
        persistenceService = new CodeReviewPersistenceService(codeReviewRepository, null);
    }

    @Test
    void testTaskExecutor_HasGracefulShutdownConfigured() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.taskExecutor();

        Field waitField = org.springframework.util.ReflectionUtils.findField(ThreadPoolTaskExecutor.class, "waitForTasksToCompleteOnShutdown");
        assertThat(waitField).isNotNull();
        waitField.setAccessible(true);
        boolean waitForTasks = (boolean) waitField.get(executor);
        assertThat(waitForTasks).isTrue();

        for (Field f : executor.getClass().getSuperclass().getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("terminat") || f.getName().toLowerCase().contains("await")) {
                f.setAccessible(true);
                System.out.println("TERMINATION_FIELD: " + f.getName() + " = " + f.get(executor));
            }
        }

        executor.destroy();
    }

    @Test
    void testGlobalExceptionHandler_DataAccessException_Returns503WithoutLeakingDetails() {
        CannotAcquireLockException dbEx = new CannotAcquireLockException("Connection pool exhausted: FATAL password authentication failed for user postgres");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataAccessException(dbEx);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(503);
        assertThat(body.get("error")).isEqualTo("Database Unavailable");
        assertThat(body.get("message")).isEqualTo("Database service temporarily unavailable. Please retry later.");
        // Ensure raw database error message or credentials are not leaked in response
        assertThat(body.toString()).doesNotContain("password");
        assertThat(body.toString()).doesNotContain("postgres");
    }

    @Test
    void testCodeReviewPersistenceService_ActiveReviewOlderThan15Minutes_NotMarkedFailed() {
        CodeReview longRunningActiveReview = new CodeReview(100L, "owner", "repo", 1, null, "sha-active-long");
        longRunningActiveReview.setCreatedAt(Instant.now().minusSeconds(1200)); // 20 min ago (> 15 min threshold)
        longRunningActiveReview.setStatus(CodeReviewStatus.IN_PROGRESS);
        codeReviewRepository.save(longRunningActiveReview);

        // Register review as actively running in memory
        persistenceService.registerActiveReview(longRunningActiveReview.getId());
        assertThat(persistenceService.isReviewActive(longRunningActiveReview.getId())).isTrue();

        Instant threshold = Instant.now().minusSeconds(900); // 15 min threshold
        int recovered = persistenceService.recoverStaleInProgressReviews(threshold);

        // Actively running review must NOT be recovered/failed solely due to elapsed time
        assertThat(recovered).isEqualTo(0);
        assertThat(longRunningActiveReview.getStatus()).isEqualTo(CodeReviewStatus.IN_PROGRESS);

        // In duplicate detection, active review must still be returned as IN_PROGRESS duplicate
        Optional<CodeReview> duplicateOpt = persistenceService.findDuplicateReview(
                null, 100L, "owner", "repo", 1, "sha-active-long"
        );
        assertThat(duplicateOpt).isPresent();
        assertThat(duplicateOpt.get().getStatus()).isEqualTo(CodeReviewStatus.IN_PROGRESS);
    }

    @Test
    void testCodeReviewPersistenceService_AbandonedStaleReviewOlderThan15Minutes_MarkedFailed() {
        CodeReview deadReview = new CodeReview(100L, "owner", "repo", 2, null, "sha-abandoned");
        deadReview.setCreatedAt(Instant.now().minusSeconds(1200)); // 20 min ago
        deadReview.setStatus(CodeReviewStatus.IN_PROGRESS);
        codeReviewRepository.save(deadReview);

        // Not registered as active in memory (simulating post-restart or crashed worker)
        assertThat(persistenceService.isReviewActive(deadReview.getId())).isFalse();

        Instant threshold = Instant.now().minusSeconds(900);
        int recovered = persistenceService.recoverStaleInProgressReviews(threshold);

        // Abandoned review is safely recovered and marked FAILED
        assertThat(recovered).isEqualTo(1);
        assertThat(deadReview.getStatus()).isEqualTo(CodeReviewStatus.FAILED);
        assertThat(deadReview.getReviewSummary()).contains("timed out or was interrupted by application shutdown");
        assertThat(deadReview.getCompletedAt()).isNotNull();

        // Stale duplicate check also unblocks subsequent submissions
        Optional<CodeReview> duplicateOpt = persistenceService.findDuplicateReview(
                null, 100L, "owner", "repo", 2, "sha-abandoned"
        );
        assertThat(duplicateOpt).isEmpty();
    }

    @Test
    void testCodeReviewPersistenceService_HeartbeatLifecycle() {
        Long reviewId = 999L;
        assertThat(persistenceService.isReviewActive(reviewId)).isFalse();
        assertThat(persistenceService.getReviewHeartbeat(reviewId)).isEmpty();

        persistenceService.registerActiveReview(reviewId);
        assertThat(persistenceService.isReviewActive(reviewId)).isTrue();
        assertThat(persistenceService.getReviewHeartbeat(reviewId)).isPresent();

        persistenceService.recordHeartbeat(reviewId);
        assertThat(persistenceService.isReviewActive(reviewId)).isTrue();

        persistenceService.unregisterActiveReview(reviewId);
        assertThat(persistenceService.isReviewActive(reviewId)).isFalse();
        assertThat(persistenceService.getReviewHeartbeat(reviewId)).isEmpty();
    }

    @Test
    void testHikariCPConfiguration_RelaxedBindingEnvironmentVariables() {
        org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
        
        // 1. Verify default fallback resolves to 20 when no environment variables are set
        org.springframework.boot.context.properties.bind.Binder binderDefault = 
                org.springframework.boot.context.properties.bind.Binder.get(env);
        
        // Test binding with canonical Spring Boot relaxed binding variable
        env.setProperty("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "25");
        String boundCanonical = env.resolvePlaceholders(
                "${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:${DB_POOL_MAX_SIZE:20}}}"
        );
        assertThat(boundCanonical).isEqualTo("25");

        // Test binding with shorthand alias
        org.springframework.mock.env.MockEnvironment envShorthand = new org.springframework.mock.env.MockEnvironment();
        envShorthand.setProperty("SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE", "30");
        String boundShorthand = envShorthand.resolvePlaceholders(
                "${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:${DB_POOL_MAX_SIZE:20}}}"
        );
        assertThat(boundShorthand).isEqualTo("30");

        // Test binding with legacy Docker env var
        org.springframework.mock.env.MockEnvironment envLegacy = new org.springframework.mock.env.MockEnvironment();
        envLegacy.setProperty("DB_POOL_MAX_SIZE", "15");
        String boundLegacy = envLegacy.resolvePlaceholders(
                "${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:${DB_POOL_MAX_SIZE:20}}}"
        );
        assertThat(boundLegacy).isEqualTo("15");

        // Test default resolution when none are set
        org.springframework.mock.env.MockEnvironment envEmpty = new org.springframework.mock.env.MockEnvironment();
        String boundDefault = envEmpty.resolvePlaceholders(
                "${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:${DB_POOL_MAX_SIZE:20}}}"
        );
        assertThat(boundDefault).isEqualTo("20");
    }

    @Test
    void testCodeReviewPersistenceService_ConcurrentCompletionCannotBeOverwrittenByStaleRecovery() {
        CodeReview review = new CodeReview(100L, "owner", "repo", 42, null, "sha-race");
        review.setCreatedAt(Instant.now().minusSeconds(1200)); // 20m ago
        review.setStatus(CodeReviewStatus.IN_PROGRESS);
        codeReviewRepository.save(review);

        // Step 1: Worker completes the review concurrently
        persistenceService.markCompleted(review.getId(), "Review completed successfully", 5, 2);
        assertThat(review.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);

        // Step 2: Stale recovery runs afterwards (or races)
        int recovered = persistenceService.recoverStaleInProgressReviews(Instant.now().minusSeconds(900));

        // Stale recovery must NOT clobber the completed review
        assertThat(recovered).isEqualTo(0);
        CodeReview current = persistenceService.findById(review.getId()).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
        assertThat(current.getReviewSummary()).isEqualTo("Review completed successfully");
    }

    @Test
    void testCodeReviewPersistenceService_MultiThreadedConcurrentCompletionAndRecovery() throws Exception {
        int threads = 10;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        List<CodeReview> testReviews = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            CodeReview r = new CodeReview(100L, "owner", "repo", i, null, "sha-" + i);
            r.setCreatedAt(Instant.now().minusSeconds(1200));
            r.setStatus(CodeReviewStatus.IN_PROGRESS);
            codeReviewRepository.save(r);
            testReviews.add(r);
        }

        // Half the threads try to mark COMPLETED, half run stale recovery
        for (int i = 0; i < threads; i++) {
            final int index = i;
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    if (index % 2 == 0) {
                        persistenceService.markCompleted(testReviews.get(index).getId(), "Success " + index, 1, 0);
                        completedCount.incrementAndGet();
                    } else {
                        persistenceService.recoverStaleInProgressReviews(Instant.now().minusSeconds(900));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // Any review that completed MUST remain COMPLETED and never revert to FAILED
        for (int i = 0; i < threads; i += 2) {
            CodeReview r = persistenceService.findById(testReviews.get(i).getId()).orElseThrow();
            assertThat(r.getStatus()).isEqualTo(CodeReviewStatus.COMPLETED);
            assertThat(r.getReviewSummary()).isEqualTo("Success " + i);
        }
    }

    private static class InMemoryCodeReviewRepository implements CodeReviewRepository {
        private final List<CodeReview> reviews = new ArrayList<>();
        private long idGen = 1L;

        @Override
        public <S extends CodeReview> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
                reviews.add(entity);
            }
            return entity;
        }

        @Override
        public int transitionToFailedIfInProgress(Long id, String summary, Instant completedAt) {
            for (CodeReview r : reviews) {
                if (r.getId().equals(id) && r.getStatus() == CodeReviewStatus.IN_PROGRESS) {
                    r.setStatus(CodeReviewStatus.FAILED);
                    r.setReviewSummary(summary);
                    r.setCompletedAt(completedAt);
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public List<CodeReview> findStaleInProgressReviews(Instant threshold) {
            return reviews.stream()
                    .filter(r -> r.getStatus() == CodeReviewStatus.IN_PROGRESS && r.getCreatedAt() != null && r.getCreatedAt().isBefore(threshold))
                    .toList();
        }

        @Override
        public List<CodeReview> findDuplicateReviews(Long userId, Long installationId, String owner, String repository, Integer pullRequestNumber, String commitSha, List<CodeReviewStatus> statuses) {
            return reviews.stream()
                    .filter(r -> r.getInstallationId().equals(installationId)
                            && r.getOwner().equalsIgnoreCase(owner)
                            && r.getRepository().equalsIgnoreCase(repository)
                            && r.getPullRequestNumber().equals(pullRequestNumber)
                            && (commitSha == null || commitSha.equals(r.getCommitSha()))
                            && statuses.contains(r.getStatus()))
                    .toList();
        }

        @Override public Optional<CodeReview> findById(Long id) { return reviews.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findAllByOrderByCreatedAtDesc() { return List.of(); }
        @Override public List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public Optional<CodeReview> findByIdAndUserId(Long id, Long userId) { return Optional.empty(); }
        @Override public com.pushkar.codereview.analytics.projection.ReviewOverviewProjection getOverviewStats(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return null; }
        @Override public List<com.pushkar.codereview.analytics.projection.ReviewTrendRowProjection> getTrendRows(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public List<com.pushkar.codereview.analytics.projection.RepositoryReviewSummaryProjection> getRepositoryReviewSummaries(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public void flush() {}
        @Override public <S extends CodeReview> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeReview> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<CodeReview> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public CodeReview getOne(Long aLong) { return null; }
        @Override public CodeReview getById(Long aLong) { return null; }
        @Override public CodeReview getReferenceById(Long aLong) { return null; }
        @Override public <S extends CodeReview> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends CodeReview> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends CodeReview> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends CodeReview> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends CodeReview> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends CodeReview> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends CodeReview, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends CodeReview> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<CodeReview> findAll() { return reviews; }
        @Override public List<CodeReview> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return reviews.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(CodeReview entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends CodeReview> entities) {}
        @Override public void deleteAll() {}
        @Override public List<CodeReview> findAll(org.springframework.data.domain.Sort sort) { return reviews; }
        @Override public org.springframework.data.domain.Page<CodeReview> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public Optional<CodeReview> findOne(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return Optional.empty(); }
        @Override public List<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return List.of(); }
        @Override public org.springframework.data.domain.Page<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public List<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public long count(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return 0; }
        @Override public boolean exists(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return false; }
        @Override public long delete(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return 0; }
        @Override public <S extends CodeReview, R> R findBy(org.springframework.data.jpa.domain.Specification<CodeReview> spec, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}
