package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.policy.RepositoryPolicyService;
import com.pushkar.codereview.policy.RepositoryReviewPolicy;
import com.pushkar.codereview.qualitygate.dto.QualityGateResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualityGateServiceTest {

    private InMemoryCodeReviewRepository reviewRepository;
    private InMemoryCodeReviewFindingRepository findingRepository;
    private InMemoryQualityGateRepository qualityGateRepository;
    private QualityGateEvaluator evaluator;
    private RepositoryPolicyService policyService;
    private StubCurrentUserService currentUserService;
    private QualityGateService service;

    private User user1;
    private User user2;
    private User adminUser;
    private Repository repository;
    private CodeReview review;

    @BeforeEach
    void setUp() {
        reviewRepository = new InMemoryCodeReviewRepository();
        findingRepository = new InMemoryCodeReviewFindingRepository();
        qualityGateRepository = new InMemoryQualityGateRepository();
        evaluator = new QualityGateEvaluator();
        StubRepositoryPolicyService stubPolicyService = new StubRepositoryPolicyService();
        policyService = stubPolicyService;
        currentUserService = new StubCurrentUserService();

        service = new QualityGateService(
                reviewRepository,
                findingRepository,
                qualityGateRepository,
                evaluator,
                policyService,
                currentUserService
        );

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);

        adminUser = new User("admin", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);

        repository = new Repository(user1, 100L, "my-repo", "user1/my-repo", "main", "https://github.com/user1/my-repo", true);
        repository.setId(1L);

        review = new CodeReview(12345L, "user1", "my-repo", 1, user1);
        review.setId(101L);
        review.setStatus(CodeReviewStatus.COMPLETED);
        reviewRepository.save(review);

        RepositoryReviewPolicy defaultPolicy = RepositoryReviewPolicy.createDefault(repository);
        stubPolicyService.setDefaultPolicy(defaultPolicy);
    }

    @Test
    void testGetOrEvaluateQualityGate_CompletedReviewWithHighFinding_FailsAndPersists() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        CodeReviewFinding highFinding = new CodeReviewFinding(
                review, "Database.java", 45, 50,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.SECURITY,
                "SQL Injection vulnerability", "Use prepared statement"
        );
        highFinding.setId(501L);
        findingRepository.save(highFinding);

        QualityGateResponse response = service.getOrEvaluateQualityGate(review.getId());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(QualityGateStatus.FAIL);
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getReason()).contains("Quality gate failed: 1 finding(s)");

        // Verify persisted
        Optional<CodeReviewQualityGate> persisted = qualityGateRepository.findByCodeReviewId(review.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(QualityGateStatus.FAIL);
    }

    @Test
    void testGetOrEvaluateQualityGate_DuplicatePrevention_ReturnsPersistedResult() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        // First call evaluates and persists
        QualityGateResponse firstResponse = service.getOrEvaluateQualityGate(review.getId());
        assertThat(firstResponse.getStatus()).isEqualTo(QualityGateStatus.PASS);

        // Modify repository findings after the fact to verify historical reproducibility
        CodeReviewFinding lateFinding = new CodeReviewFinding(
                review, "Exploit.java", 1, 5,
                ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY,
                "New critical bug", "Fix"
        );
        findingRepository.save(lateFinding);

        // Second call should return previously persisted PASS result without recalculation
        QualityGateResponse secondResponse = service.getOrEvaluateQualityGate(review.getId());
        assertThat(secondResponse.getStatus()).isEqualTo(QualityGateStatus.PASS);
        assertThat(secondResponse.getId()).isEqualTo(firstResponse.getId());
    }

    @Test
    void testGetOrEvaluateQualityGate_InProgressReview_ReturnsNotEvaluatedWithoutSaving() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        review.setStatus(CodeReviewStatus.IN_PROGRESS);
        reviewRepository.save(review);

        QualityGateResponse response = service.getOrEvaluateQualityGate(review.getId());

        assertThat(response.getStatus()).isEqualTo(QualityGateStatus.NOT_EVALUATED);
        assertThat(response.getReason()).contains("Review is in progress");

        // Verify NOT saved to database
        assertThat(qualityGateRepository.existsByCodeReviewId(review.getId())).isFalse();
    }

    @Test
    void testGetOrEvaluateQualityGate_FailedReview_ReturnsNotEvaluatedWithoutSaving() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        review.setStatus(CodeReviewStatus.FAILED);
        reviewRepository.save(review);

        QualityGateResponse response = service.getOrEvaluateQualityGate(review.getId());

        assertThat(response.getStatus()).isEqualTo(QualityGateStatus.NOT_EVALUATED);
        assertThat(response.getReason()).contains("Review execution failed");

        // Verify NOT saved to database
        assertThat(qualityGateRepository.existsByCodeReviewId(review.getId())).isFalse();
    }

    @Test
    void testAuthorization_CrossUserAccess_ThrowsAccessDeniedException() {
        // User 2 tries to access User 1's review quality gate
        currentUserService.setContext(user2.getId(), "user2@example.com", "USER");

        assertThatThrownBy(() -> service.getOrEvaluateQualityGate(review.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to access this code review");
    }

    @Test
    void testAuthorization_AdminCanAccessAnyReviewQualityGate() {
        currentUserService.setContext(adminUser.getId(), "admin@example.com", "ADMIN");

        QualityGateResponse response = service.getOrEvaluateQualityGate(review.getId());
        assertThat(response).isNotNull();
        assertThat(response.getReviewId()).isEqualTo(review.getId());
    }

    // ==========================================
    // Test Doubles & Helpers
    // ==========================================

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;

        StubCurrentUserService() {
            super(null);
        }

        void setContext(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        @Override public boolean isAuthenticated() { return username != null; }
        @Override public String getCurrentUsername() { return username; }
        @Override public Long getCurrentUserId() { return userId; }
        @Override public boolean hasRole(String targetRole) {
            if (role == null) return false;
            String clean = targetRole.toUpperCase().replace("ROLE_", "");
            return clean.equalsIgnoreCase(this.role.replace("ROLE_", ""));
        }
    }

    private static class InMemoryCodeReviewRepository implements CodeReviewRepository {
        final Map<Long, CodeReview> database = new HashMap<>();

        @Override public Optional<CodeReview> findById(Long id) { return Optional.ofNullable(database.get(id)); }
        @Override public <S extends CodeReview> S save(S entity) { database.put(entity.getId(), entity); return entity; }
        @Override public boolean existsById(Long id) { return database.containsKey(id); }
        @Override public List<CodeReview> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findAllByOrderByCreatedAtDesc() { return List.of(); }
        @Override public List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public List<CodeReview> findDuplicateReviews(Long userId, Long installationId, String owner, String repository, Integer pullRequestNumber, String commitSha, List<CodeReviewStatus> statuses) { return List.of(); }
        @Override public Optional<CodeReview> findByIdAndUserId(Long id, Long userId) { return Optional.ofNullable(database.get(id)); }
        @Override public com.pushkar.codereview.analytics.projection.ReviewOverviewProjection getOverviewStats(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return null; }
        @Override public List<com.pushkar.codereview.analytics.projection.ReviewTrendRowProjection> getTrendRows(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public List<com.pushkar.codereview.analytics.projection.RepositoryReviewSummaryProjection> getRepositoryReviewSummaries(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public void flush() {}
        @Override public <S extends CodeReview> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeReview> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<CodeReview> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public CodeReview getOne(Long aLong) { return database.get(aLong); }
        @Override public CodeReview getById(Long aLong) { return database.get(aLong); }
        @Override public CodeReview getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends CodeReview> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReview> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<CodeReview> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(CodeReview entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends CodeReview> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<CodeReview> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<CodeReview> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
        @Override public Optional<CodeReview> findOne(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return Optional.empty(); }
        @Override public List<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return new ArrayList<>(database.values()); }
        @Override public Page<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec, Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
        @Override public List<CodeReview> findAll(org.springframework.data.jpa.domain.Specification<CodeReview> spec, Sort sort) { return new ArrayList<>(database.values()); }
        @Override public long count(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return database.size(); }
        @Override public boolean exists(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return !database.isEmpty(); }
        @Override public long delete(org.springframework.data.jpa.domain.Specification<CodeReview> spec) { return 0; }
        @Override public <S extends CodeReview, R> R findBy(org.springframework.data.jpa.domain.Specification<CodeReview> spec, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryCodeReviewFindingRepository implements CodeReviewFindingRepository {
        final Map<Long, CodeReviewFinding> database = new HashMap<>();
        private long idGen = 1L;

        @Override public List<CodeReviewFinding> findByCodeReviewIdOrderByFilePathAscLineNumberAsc(Long codeReviewId) {
            return database.values().stream().filter(f -> f.getCodeReview() != null && codeReviewId.equals(f.getCodeReview().getId())).toList();
        }

        @Override public Page<CodeReviewFinding> findByCodeReviewId(Long codeReviewId, Pageable pageable) {
            List<CodeReviewFinding> list = findByCodeReviewIdOrderByFilePathAscLineNumberAsc(codeReviewId);
            return new PageImpl<>(list, pageable, list.size());
        }

        @Override
        public <S extends CodeReviewFinding> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public List<com.pushkar.codereview.analytics.projection.FindingSeverityGroupProjection> countFindingsBySeverity(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public List<com.pushkar.codereview.analytics.projection.FindingCategoryGroupProjection> countFindingsByCategory(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public long countRuleBasedFindings(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return 0; }
        @Override public long countTotalFindings(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return database.size(); }
        @Override public List<com.pushkar.codereview.analytics.projection.RepositoryFindingSeverityProjection> countRepositoryFindingsBySeverity(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) { return List.of(); }
        @Override public void flush() {}
        @Override public <S extends CodeReviewFinding> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeReviewFinding> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<CodeReviewFinding> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public CodeReviewFinding getOne(Long aLong) { return database.get(aLong); }
        @Override public CodeReviewFinding getById(Long aLong) { return database.get(aLong); }
        @Override public CodeReviewFinding getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends CodeReviewFinding> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewFinding> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<CodeReviewFinding> findById(Long aLong) { return Optional.ofNullable(database.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return database.containsKey(aLong); }
        @Override public List<CodeReviewFinding> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<CodeReviewFinding> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(CodeReviewFinding entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends CodeReviewFinding> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<CodeReviewFinding> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<CodeReviewFinding> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
    }

    private static class InMemoryQualityGateRepository implements CodeReviewQualityGateRepository {
        final Map<Long, CodeReviewQualityGate> database = new HashMap<>();
        private long idGen = 1L;

        @Override
        public Optional<CodeReviewQualityGate> findByCodeReviewId(Long codeReviewId) {
            return database.values().stream()
                    .filter(g -> g.getCodeReview() != null && codeReviewId.equals(g.getCodeReview().getId()))
                    .findFirst();
        }

        @Override
        public boolean existsByCodeReviewId(Long codeReviewId) {
            return findByCodeReviewId(codeReviewId).isPresent();
        }

        @Override
        public <S extends CodeReviewQualityGate> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends CodeReviewQualityGate> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<CodeReviewQualityGate> findById(Long aLong) { return Optional.ofNullable(database.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return database.containsKey(aLong); }
        @Override public List<CodeReviewQualityGate> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<CodeReviewQualityGate> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(CodeReviewQualityGate entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends CodeReviewQualityGate> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<CodeReviewQualityGate> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<CodeReviewQualityGate> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
        @Override public void flush() {}
        @Override public <S extends CodeReviewQualityGate> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeReviewQualityGate> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<CodeReviewQualityGate> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public CodeReviewQualityGate getOne(Long aLong) { return database.get(aLong); }
        @Override public CodeReviewQualityGate getById(Long aLong) { return database.get(aLong); }
        @Override public CodeReviewQualityGate getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends CodeReviewQualityGate> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CodeReviewQualityGate, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class StubRepositoryPolicyService extends RepositoryPolicyService {
        private RepositoryReviewPolicy defaultPolicy;

        public StubRepositoryPolicyService() {
            super(null, null, null, null);
        }

        public void setDefaultPolicy(RepositoryReviewPolicy policy) {
            this.defaultPolicy = policy;
        }

        @Override
        public RepositoryReviewPolicy resolveEffectivePolicy(String owner, String repositoryName) {
            return defaultPolicy != null ? defaultPolicy : RepositoryReviewPolicy.createDefault(null);
        }
    }
}
