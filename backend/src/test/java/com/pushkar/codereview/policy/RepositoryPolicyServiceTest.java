package com.pushkar.codereview.policy;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.rule.CodeQualityRule;
import com.pushkar.codereview.github.review.rule.ReviewAnalysisContext;
import com.pushkar.codereview.github.review.rule.RuleFinding;
import com.pushkar.codereview.github.review.rule.RuleRegistry;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyRequest;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
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
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryPolicyServiceTest {

    private InMemoryRepositoryPolicyRepository policyRepository;
    private InMemoryRepositoryRepository repositoryRepository;
    private RuleRegistry ruleRegistry;
    private StubCurrentUserService currentUserService;
    private RepositoryPolicyService service;

    private User user1;
    private User user2;
    private User adminUser;
    private Repository repo1;

    @BeforeEach
    void setUp() {
        policyRepository = new InMemoryRepositoryPolicyRepository();
        repositoryRepository = new InMemoryRepositoryRepository();
        currentUserService = new StubCurrentUserService();

        CodeQualityRule rule1 = new DummyRule("RULE-JAVA-SYSTEM-OUT", "System.out", ReviewFindingCategory.CODE_STYLE, ReviewFindingSeverity.LOW);
        CodeQualityRule rule2 = new DummyRule("RULE-JAVA-EMPTY-CATCH", "Empty Catch", ReviewFindingCategory.BUG, ReviewFindingSeverity.MEDIUM);
        CodeQualityRule rule3 = new DummyRule("RULE-TODO-FIXME", "TODO Markers", ReviewFindingCategory.MAINTAINABILITY, ReviewFindingSeverity.LOW);
        ruleRegistry = new RuleRegistry(List.of(rule1, rule2, rule3));

        service = new RepositoryPolicyService(policyRepository, repositoryRepository, ruleRegistry, currentUserService);

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);

        adminUser = new User("admin", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);

        repo1 = new Repository(user1, 10001L, "bot-core", "user1/bot-core", "main", "https://github.com/user1/bot-core", true);
        repo1.setId(1L);
        repositoryRepository.save(repo1);
    }

    @Test
    void testGetEffectivePolicy_WhenNoCustomPolicyExists_ReturnsDefaultPolicy() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyResponse response = service.getEffectivePolicy(repo1.getId());

        assertThat(response).isNotNull();
        assertThat(response.getRepositoryId()).isEqualTo(repo1.getId());
        assertThat(response.getRepositoryFullName()).isEqualTo("user1/bot-core");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(response.getEnabledRuleIds()).containsExactlyInAnyOrder(
                "RULE-JAVA-SYSTEM-OUT", "RULE-JAVA-EMPTY-CATCH", "RULE-TODO-FIXME"
        );
        assertThat(response.isCustom()).isFalse();
        assertThat(response.getAvailableRules()).hasSize(3);
    }

    @Test
    void testUpdatePolicy_CreatesAndPersistsCustomPolicy() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true,
                ReviewFindingSeverity.CRITICAL,
                Set.of("RULE-JAVA-SYSTEM-OUT")
        );

        RepositoryReviewPolicyResponse response = service.updatePolicy(repo1.getId(), request);

        assertThat(response.isCustom()).isTrue();
        assertThat(response.getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.CRITICAL);
        assertThat(response.getEnabledRuleIds()).containsExactly("RULE-JAVA-SYSTEM-OUT");

        // Verify persisted in repository
        Optional<RepositoryReviewPolicy> persisted = policyRepository.findByRepositoryId(repo1.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.CRITICAL);
        assertThat(persisted.get().getEnabledRuleIdsSet()).containsExactly("RULE-JAVA-SYSTEM-OUT");
    }

    @Test
    void testUpdatePolicy_OnePolicyPerRepository_UpdatesExistingWithoutDuplicates() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyRequest req1 = new RepositoryReviewPolicyRequest(
                true, ReviewFindingSeverity.MEDIUM, Set.of("RULE-JAVA-EMPTY-CATCH")
        );
        service.updatePolicy(repo1.getId(), req1);

        RepositoryReviewPolicyRequest req2 = new RepositoryReviewPolicyRequest(
                false, ReviewFindingSeverity.LOW, Set.of("RULE-TODO-FIXME")
        );
        RepositoryReviewPolicyResponse response2 = service.updatePolicy(repo1.getId(), req2);

        assertThat(response2.isEnabled()).isFalse();
        assertThat(response2.getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
        assertThat(response2.getEnabledRuleIds()).containsExactly("RULE-TODO-FIXME");

        // Database should only have 1 row for repo1
        long countForRepo = policyRepository.database.values().stream()
                .filter(p -> p.getRepository().getId().equals(repo1.getId()))
                .count();
        assertThat(countForRepo).isEqualTo(1);
    }

    @Test
    void testResetPolicy_RestoresDefaultValues() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        // First modify it
        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                false, ReviewFindingSeverity.LOW, Set.of()
        );
        service.updatePolicy(repo1.getId(), request);

        // Now reset it
        RepositoryReviewPolicyResponse resetResponse = service.resetPolicy(repo1.getId());

        assertThat(resetResponse.isEnabled()).isTrue();
        assertThat(resetResponse.getFailOnSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(resetResponse.getEnabledRuleIds()).containsExactlyInAnyOrder(
                "RULE-JAVA-SYSTEM-OUT", "RULE-JAVA-EMPTY-CATCH", "RULE-TODO-FIXME"
        );
    }

    @Test
    void testValidation_UnsupportedRuleId_ThrowsIllegalArgumentException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true, ReviewFindingSeverity.HIGH, Set.of("RULE-NONEXISTENT-ARBITRARY")
        );

        assertThatThrownBy(() -> service.updatePolicy(repo1.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported rule ID 'RULE-NONEXISTENT-ARBITRARY'");
    }

    @Test
    void testValidation_NullSeverity_ThrowsIllegalArgumentException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true, null, Set.of("RULE-JAVA-SYSTEM-OUT")
        );

        assertThatThrownBy(() -> service.updatePolicy(repo1.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failOnSeverity must not be null");
    }

    @Test
    void testEmptyEnabledRuleList_ExplicitlyDisablesAllDeterministicRules() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        RepositoryReviewPolicyRequest request = new RepositoryReviewPolicyRequest(
                true, ReviewFindingSeverity.HIGH, Set.of()
        );

        RepositoryReviewPolicyResponse response = service.updatePolicy(repo1.getId(), request);

        assertThat(response.getEnabledRuleIds()).isEmpty();

        RepositoryReviewPolicy persisted = policyRepository.findByRepositoryId(repo1.getId()).orElseThrow();
        assertThat(persisted.getEnabledRuleIdsSet()).isEmpty();
    }

    @Test
    void testAuthorization_CrossUserAccess_ThrowsResourceNotFoundException() {
        // User 2 attempts to view User 1's repository policy
        currentUserService.setContext(user2.getId(), "user2@example.com", "USER");

        assertThatThrownBy(() -> service.getEffectivePolicy(repo1.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with ID: " + repo1.getId());
    }

    @Test
    void testAuthorization_AdminCanAccessAnyRepositoryPolicy() {
        currentUserService.setContext(adminUser.getId(), "admin@example.com", "ADMIN");

        RepositoryReviewPolicyResponse response = service.getEffectivePolicy(repo1.getId());
        assertThat(response).isNotNull();
        assertThat(response.getRepositoryId()).isEqualTo(repo1.getId());
    }

    @Test
    void testAuthorization_UnauthenticatedUser_ThrowsAccessDeniedException() {
        currentUserService.clearContext();

        assertThatThrownBy(() -> service.getEffectivePolicy(repo1.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Full authentication is required");
    }

    // ==========================================
    // Test Doubles & Helpers
    // ==========================================

    private static class DummyRule implements CodeQualityRule {
        private final String ruleId;
        private final String name;
        private final ReviewFindingCategory category;
        private final ReviewFindingSeverity severity;

        DummyRule(String ruleId, String name, ReviewFindingCategory category, ReviewFindingSeverity severity) {
            this.ruleId = ruleId;
            this.name = name;
            this.category = category;
            this.severity = severity;
        }

        @Override public String getRuleId() { return ruleId; }
        @Override public String getName() { return name; }
        @Override public String getDescription() { return name + " description"; }
        @Override public ReviewFindingCategory getCategory() { return category; }
        @Override public ReviewFindingSeverity getSeverity() { return severity; }
        @Override public List<RuleFinding> evaluate(ReviewAnalysisContext context) { return List.of(); }
    }

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

        void clearContext() {
            this.userId = null;
            this.username = null;
            this.role = null;
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

    private static class InMemoryRepositoryPolicyRepository implements RepositoryReviewPolicyRepository {
        final Map<Long, RepositoryReviewPolicy> database = new HashMap<>();
        private long idGen = 1L;

        @Override
        public Optional<RepositoryReviewPolicy> findByRepositoryId(Long repositoryId) {
            return database.values().stream()
                    .filter(p -> p.getRepository() != null && repositoryId.equals(p.getRepository().getId()))
                    .findFirst();
        }

        @Override
        public boolean existsByRepositoryId(Long repositoryId) {
            return findByRepositoryId(repositoryId).isPresent();
        }

        @Override
        public void deleteByRepositoryId(Long repositoryId) {
            database.values().removeIf(p -> p.getRepository() != null && repositoryId.equals(p.getRepository().getId()));
        }

        @Override
        public <S extends RepositoryReviewPolicy> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends RepositoryReviewPolicy> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<RepositoryReviewPolicy> findById(Long aLong) { return Optional.ofNullable(database.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return database.containsKey(aLong); }
        @Override public List<RepositoryReviewPolicy> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<RepositoryReviewPolicy> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(RepositoryReviewPolicy entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends RepositoryReviewPolicy> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<RepositoryReviewPolicy> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<RepositoryReviewPolicy> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
        @Override public void flush() {}
        @Override public <S extends RepositoryReviewPolicy> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends RepositoryReviewPolicy> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<RepositoryReviewPolicy> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public RepositoryReviewPolicy getOne(Long aLong) { return database.get(aLong); }
        @Override public RepositoryReviewPolicy getById(Long aLong) { return database.get(aLong); }
        @Override public RepositoryReviewPolicy getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends RepositoryReviewPolicy> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends RepositoryReviewPolicy, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryRepositoryRepository implements RepositoryRepository {
        final Map<Long, Repository> database = new HashMap<>();
        private long idGen = 1L;

        @Override public boolean existsByGithubRepositoryId(Long githubRepositoryId) { return database.values().stream().anyMatch(r -> githubRepositoryId.equals(r.getGithubRepositoryId())); }
        @Override public List<Repository> findByUserId(Long userId) { return database.values().stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getId())).toList(); }
        @Override public Optional<Repository> findByIdAndUserId(Long id, Long userId) { return database.values().stream().filter(r -> r.getId().equals(id) && r.getUser() != null && userId.equals(r.getUser().getId())).findFirst(); }
        @Override public Optional<Repository> findByGithubRepositoryId(Long githubRepositoryId) { return database.values().stream().filter(r -> githubRepositoryId != null && githubRepositoryId.equals(r.getGithubRepositoryId())).findFirst(); }
        @Override public Optional<Repository> findByFullNameIgnoreCase(String fullName) { return database.values().stream().filter(r -> fullName != null && fullName.equalsIgnoreCase(r.getFullName())).findFirst(); }
        @Override public List<Repository> findByNameIgnoreCase(String name) { return database.values().stream().filter(r -> name != null && name.equalsIgnoreCase(r.getName())).toList(); }

        @Override
        public <S extends Repository> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends Repository> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<Repository> findById(Long aLong) { return Optional.ofNullable(database.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return database.containsKey(aLong); }
        @Override public List<Repository> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<Repository> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(Repository entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Repository> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<Repository> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<Repository> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
        @Override public void flush() {}
        @Override public <S extends Repository> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Repository> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Repository> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Repository getOne(Long aLong) { return database.get(aLong); }
        @Override public Repository getById(Long aLong) { return database.get(aLong); }
        @Override public Repository getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends Repository> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
