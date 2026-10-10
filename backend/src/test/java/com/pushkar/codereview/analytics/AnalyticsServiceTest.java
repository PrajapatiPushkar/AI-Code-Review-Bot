package com.pushkar.codereview.analytics;

import com.pushkar.codereview.analytics.dto.AnalyticsFindingsResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsOverviewResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsTrendItemResponse;
import com.pushkar.codereview.analytics.dto.RepositoryHealthStatus;
import com.pushkar.codereview.analytics.dto.RepositoryHealthSummaryResponse;
import com.pushkar.codereview.analytics.projection.FindingCategoryGroupProjection;
import com.pushkar.codereview.analytics.projection.FindingSeverityGroupProjection;
import com.pushkar.codereview.analytics.projection.RepositoryFindingSeverityProjection;
import com.pushkar.codereview.analytics.projection.RepositoryReviewSummaryProjection;
import com.pushkar.codereview.analytics.projection.ReviewOverviewProjection;
import com.pushkar.codereview.analytics.projection.ReviewTrendRowProjection;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsServiceTest {

    private InMemoryCodeReviewRepository reviewRepository;
    private InMemoryCodeReviewFindingRepository findingRepository;
    private InMemoryRepositoryRepository repositoryRepository;
    private TestCurrentUserService currentUserService;
    private AnalyticsService analyticsService;

    private User user1;
    private User user2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        reviewRepository = new InMemoryCodeReviewRepository();
        findingRepository = new InMemoryCodeReviewFindingRepository();
        repositoryRepository = new InMemoryRepositoryRepository();
        currentUserService = new TestCurrentUserService();

        analyticsService = new AnalyticsService(
                reviewRepository,
                findingRepository,
                repositoryRepository,
                currentUserService
        );

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);

        adminUser = new User("adminuser", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);
    }

    @AfterEach
    void tearDown() {
        currentUserService.clear();
    }

    @Test
    void testGetOverview_ReturnsAggregatedMetrics() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        CodeReview r1 = new CodeReview(100L, "org", "repo-a", 1, user1);
        r1.setId(1L);
        r1.setStatus(CodeReviewStatus.COMPLETED);
        r1.setTotalFindings(3);
        r1.setCreatedAt(Instant.parse("2026-10-01T10:00:00Z"));
        reviewRepository.save(r1);

        CodeReview r2 = new CodeReview(100L, "org", "repo-a", 2, user1);
        r2.setId(2L);
        r2.setStatus(CodeReviewStatus.FAILED);
        r2.setTotalFindings(0);
        r2.setCreatedAt(Instant.parse("2026-10-02T10:00:00Z"));
        reviewRepository.save(r2);

        CodeReview r3 = new CodeReview(100L, "org", "repo-a", 3, user1);
        r3.setId(3L);
        r3.setStatus(CodeReviewStatus.IN_PROGRESS);
        r3.setTotalFindings(0);
        r3.setCreatedAt(Instant.parse("2026-10-03T10:00:00Z"));
        reviewRepository.save(r3);

        findingRepository.saveFinding(r1, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Bug 1");
        findingRepository.saveFinding(r1, ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.SECURITY, "Sec 1");
        findingRepository.saveFinding(r1, ReviewFindingSeverity.LOW, ReviewFindingCategory.CODE_STYLE, "Style 1");

        AnalyticsOverviewResponse overview = analyticsService.getOverview(null, null, null, null);

        assertThat(overview.getTotalReviews()).isEqualTo(3);
        assertThat(overview.getCompletedReviews()).isEqualTo(1);
        assertThat(overview.getFailedReviews()).isEqualTo(1);
        assertThat(overview.getInProgressReviews()).isEqualTo(1);
        assertThat(overview.getTotalFindings()).isEqualTo(3);
    }

    @Test
    void testGetOverview_EmptyDataset_ReturnsZeros() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        AnalyticsOverviewResponse overview = analyticsService.getOverview(null, null, null, null);

        assertThat(overview.getTotalReviews()).isEqualTo(0);
        assertThat(overview.getCompletedReviews()).isEqualTo(0);
        assertThat(overview.getFailedReviews()).isEqualTo(0);
        assertThat(overview.getInProgressReviews()).isEqualTo(0);
        assertThat(overview.getTotalFindings()).isEqualTo(0);
    }

    @Test
    void testGetFindings_SeverityCategoryAndSourceBreakdown() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        CodeReview review = new CodeReview(100L, "org", "repo-a", 1, user1);
        review.setId(1L);
        review.setStatus(CodeReviewStatus.COMPLETED);
        reviewRepository.save(review);

        // 1 AI Security Finding
        findingRepository.saveFinding(review, ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY, "Hardcoded secret detected");
        // 1 AI Bug Finding
        findingRepository.saveFinding(review, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "NullPointerException risk");
        // 1 Rule Finding: System.out/err
        findingRepository.saveFinding(review, ReviewFindingSeverity.LOW, ReviewFindingCategory.CODE_STYLE, "Avoid System.out/err logging");
        // 1 Rule Finding: TODO marker
        findingRepository.saveFinding(review, ReviewFindingSeverity.INFO, ReviewFindingCategory.MAINTAINABILITY, "Unresolved TODO/FIXME marker found");

        AnalyticsFindingsResponse findings = analyticsService.getFindings(null, null, null, null);

        assertThat(findings.getTotalFindings()).isEqualTo(4);

        // Severity
        assertThat(findings.getSeverityBreakdown().get("CRITICAL")).isEqualTo(1);
        assertThat(findings.getSeverityBreakdown().get("HIGH")).isEqualTo(1);
        assertThat(findings.getSeverityBreakdown().get("MEDIUM")).isEqualTo(0);
        assertThat(findings.getSeverityBreakdown().get("LOW")).isEqualTo(1);
        assertThat(findings.getSeverityBreakdown().get("INFO")).isEqualTo(1);

        // Category
        assertThat(findings.getCategoryBreakdown().get("SECURITY")).isEqualTo(1);
        assertThat(findings.getCategoryBreakdown().get("BUG")).isEqualTo(1);
        assertThat(findings.getCategoryBreakdown().get("CODE_STYLE")).isEqualTo(1);
        assertThat(findings.getCategoryBreakdown().get("MAINTAINABILITY")).isEqualTo(1);
        assertThat(findings.getCategoryBreakdown().get("PERFORMANCE")).isEqualTo(0);
        assertThat(findings.getCategoryBreakdown().get("OTHER")).isEqualTo(0);

        // Source: 2 AI, 2 RULE
        assertThat(findings.getSourceBreakdown().get("AI")).isEqualTo(2);
        assertThat(findings.getSourceBreakdown().get("RULE")).isEqualTo(2);
    }

    @Test
    void testGetTrends_FillsGapDatesWithZeros() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        CodeReview r1 = new CodeReview(100L, "org", "repo-a", 1, user1);
        r1.setId(1L);
        r1.setStatus(CodeReviewStatus.COMPLETED);
        r1.setTotalFindings(2);
        r1.setCreatedAt(Instant.parse("2026-10-01T12:00:00Z"));
        reviewRepository.save(r1);

        CodeReview r2 = new CodeReview(100L, "org", "repo-a", 2, user1);
        r2.setId(2L);
        r2.setStatus(CodeReviewStatus.COMPLETED);
        r2.setTotalFindings(5);
        r2.setCreatedAt(Instant.parse("2026-10-03T15:00:00Z"));
        reviewRepository.save(r2);

        List<AnalyticsTrendItemResponse> trends = analyticsService.getTrends("2026-10-01", "2026-10-03", null, null);

        assertThat(trends).hasSize(3);

        // Day 1: 2026-10-01
        assertThat(trends.get(0).getDate()).isEqualTo("2026-10-01");
        assertThat(trends.get(0).getTotalReviews()).isEqualTo(1);
        assertThat(trends.get(0).getTotalFindings()).isEqualTo(2);

        // Day 2: 2026-10-02 (gap day - zeros)
        assertThat(trends.get(1).getDate()).isEqualTo("2026-10-02");
        assertThat(trends.get(1).getTotalReviews()).isEqualTo(0);
        assertThat(trends.get(1).getTotalFindings()).isEqualTo(0);

        // Day 3: 2026-10-03
        assertThat(trends.get(2).getDate()).isEqualTo("2026-10-03");
        assertThat(trends.get(2).getTotalReviews()).isEqualTo(1);
        assertThat(trends.get(2).getTotalFindings()).isEqualTo(5);
    }

    @Test
    void testGetRepositorySummaries_DistinguishesHealthStates() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        // Repo 1: Completed with findings -> COMPLETED_WITH_FINDINGS
        CodeReview r1 = new CodeReview(100L, "org", "repo-with-findings", 1, user1);
        r1.setId(1L);
        r1.setStatus(CodeReviewStatus.COMPLETED);
        r1.setTotalFindings(1);
        r1.setCreatedAt(Instant.parse("2026-10-01T10:00:00Z"));
        reviewRepository.save(r1);
        findingRepository.saveFinding(r1, ReviewFindingSeverity.HIGH, ReviewFindingCategory.BUG, "Issue 1");

        // Repo 2: Completed with 0 findings -> NO_FINDINGS_RECORDED
        CodeReview r2 = new CodeReview(100L, "org", "clean-repo", 1, user1);
        r2.setId(2L);
        r2.setStatus(CodeReviewStatus.COMPLETED);
        r2.setTotalFindings(0);
        r2.setCreatedAt(Instant.parse("2026-10-02T10:00:00Z"));
        reviewRepository.save(r2);

        // Repo 3: Registered repository with zero reviews -> NO_REVIEWS
        Repository registeredRepo = new Repository(user1, 9999L, "unreviewed-repo", "org/unreviewed-repo", "main", "http://github.com/org/unreviewed-repo", true);
        repositoryRepository.save(registeredRepo);

        List<RepositoryHealthSummaryResponse> summaries = analyticsService.getRepositorySummaries(null, null, null, null);

        assertThat(summaries).hasSize(3);

        RepositoryHealthSummaryResponse withFindings = summaries.stream().filter(s -> s.getName().equals("repo-with-findings")).findFirst().orElseThrow();
        assertThat(withFindings.getHealthStatus()).isEqualTo(RepositoryHealthStatus.COMPLETED_WITH_FINDINGS.name());
        assertThat(withFindings.getHealthStatusDescription()).isEqualTo("Reviews completed with findings");
        assertThat(withFindings.getTotalFindings()).isEqualTo(1);
        assertThat(withFindings.getHighFindings()).isEqualTo(1);

        RepositoryHealthSummaryResponse clean = summaries.stream().filter(s -> s.getName().equals("clean-repo")).findFirst().orElseThrow();
        assertThat(clean.getHealthStatus()).isEqualTo(RepositoryHealthStatus.NO_FINDINGS_RECORDED.name());
        assertThat(clean.getHealthStatusDescription()).isEqualTo("No findings recorded");
        assertThat(clean.getTotalFindings()).isEqualTo(0);

        RepositoryHealthSummaryResponse unreviewed = summaries.stream().filter(s -> s.getName().equals("unreviewed-repo")).findFirst().orElseThrow();
        assertThat(unreviewed.getHealthStatus()).isEqualTo(RepositoryHealthStatus.NO_REVIEWS.name());
        assertThat(unreviewed.getHealthStatusDescription()).isEqualTo("No reviews available");
        assertThat(unreviewed.getTotalReviews()).isEqualTo(0);
    }

    @Test
    void testCrossUserIsolation_UserCannotSeeAnotherUsersReviews() {
        // User 1 reviews
        CodeReview r1 = new CodeReview(100L, "org", "user1-private-repo", 1, user1);
        r1.setId(1L);
        r1.setStatus(CodeReviewStatus.COMPLETED);
        r1.setTotalFindings(10);
        reviewRepository.save(r1);

        // User 2 reviews
        CodeReview r2 = new CodeReview(200L, "org", "user2-private-repo", 1, user2);
        r2.setId(2L);
        r2.setStatus(CodeReviewStatus.COMPLETED);
        r2.setTotalFindings(50);
        reviewRepository.save(r2);

        // Authenticate as User 1
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        AnalyticsOverviewResponse user1Overview = analyticsService.getOverview(null, null, null, null);
        assertThat(user1Overview.getTotalReviews()).isEqualTo(1);

        // User 1 tries to query User 2's repository
        AnalyticsOverviewResponse user1QueryingUser2Repo = analyticsService.getOverview(null, null, "user2-private-repo", null);
        assertThat(user1QueryingUser2Repo.getTotalReviews()).isEqualTo(0);
        assertThat(user1QueryingUser2Repo.getTotalFindings()).isEqualTo(0);
    }

    @Test
    void testAdminAccess_AdminCanSeeAcrossAllUsers() {
        CodeReview r1 = new CodeReview(100L, "org", "repo1", 1, user1);
        r1.setId(1L);
        r1.setStatus(CodeReviewStatus.COMPLETED);
        reviewRepository.save(r1);

        CodeReview r2 = new CodeReview(200L, "org", "repo2", 1, user2);
        r2.setId(2L);
        r2.setStatus(CodeReviewStatus.COMPLETED);
        reviewRepository.save(r2);

        // Authenticate as Admin
        currentUserService.setContext(adminUser.getId(), "admin@example.com", "ADMIN");

        AnalyticsOverviewResponse adminOverview = analyticsService.getOverview(null, null, null, null);
        assertThat(adminOverview.getTotalReviews()).isEqualTo(2);
    }

    @Test
    void testDateRangeValidation_InvalidRangeThrowsIllegalArgumentException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        assertThatThrownBy(() -> analyticsService.getOverview("2026-10-15", "2026-10-01", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be after 'to'");
    }

    @Test
    void testDateFormatValidation_InvalidFormatThrowsIllegalArgumentException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER");

        assertThatThrownBy(() -> analyticsService.getOverview("not-a-date", "2026-10-01", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }

    @Test
    void testUnauthenticated_ThrowsAccessDeniedException() {
        currentUserService.clear();

        assertThatThrownBy(() -> analyticsService.getOverview(null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- In-Memory Test Repositories & Helpers ---

    private static class TestCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;

        public TestCurrentUserService() {
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

    private static class InMemoryCodeReviewRepository extends BaseStubJpaRepository<CodeReview, Long> implements CodeReviewRepository {
        @Override public <S extends CodeReview> S save(S entity) { database.put(entity.getId(), entity); return entity; }

        @Override
        public List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findAllByOrderByCreatedAtDesc() { return List.of(); }
        @Override public List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository) { return List.of(); }
        @Override public List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber) { return List.of(); }
        @Override public Optional<CodeReview> findByIdAndUserId(Long id, Long userId) { return Optional.empty(); }
        @Override public List<CodeReview> findDuplicateReviews(Long userId, Long installationId, String owner, String repository, Integer pullRequestNumber, String commitSha, List<CodeReviewStatus> statuses) { return List.of(); }

        @Override
        public ReviewOverviewProjection getOverviewStats(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReview> filtered = filterReviews(userId, owner, repository, fromInstant, toInstant);
            long total = filtered.size();
            long completed = filtered.stream().filter(r -> r.getStatus() == CodeReviewStatus.COMPLETED).count();
            long failed = filtered.stream().filter(r -> r.getStatus() == CodeReviewStatus.FAILED).count();
            long inProgress = filtered.stream().filter(r -> r.getStatus() == CodeReviewStatus.IN_PROGRESS).count();
            long totalFindings = filtered.stream().mapToLong(r -> r.getTotalFindings() != null ? r.getTotalFindings() : 0).sum();

            return new ReviewOverviewProjection() {
                @Override public Long getTotalReviews() { return total; }
                @Override public Long getCompletedReviews() { return completed; }
                @Override public Long getFailedReviews() { return failed; }
                @Override public Long getInProgressReviews() { return inProgress; }
                @Override public Long getTotalFindings() { return totalFindings; }
            };
        }

        @Override
        public List<ReviewTrendRowProjection> getTrendRows(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReview> filtered = filterReviews(userId, owner, repository, fromInstant, toInstant);
            return filtered.stream().map(r -> (ReviewTrendRowProjection) new ReviewTrendRowProjection() {
                @Override public Instant getCreatedAt() { return r.getCreatedAt(); }
                @Override public CodeReviewStatus getStatus() { return r.getStatus(); }
                @Override public Integer getTotalFindings() { return r.getTotalFindings(); }
            }).toList();
        }

        @Override
        public List<RepositoryReviewSummaryProjection> getRepositoryReviewSummaries(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReview> filtered = filterReviews(userId, owner, repository, fromInstant, toInstant);
            Map<String, List<CodeReview>> byRepo = new HashMap<>();
            for (CodeReview r : filtered) {
                byRepo.computeIfAbsent(r.getOwner() + "/" + r.getRepository(), k -> new ArrayList<>()).add(r);
            }

            List<RepositoryReviewSummaryProjection> results = new ArrayList<>();
            for (Map.Entry<String, List<CodeReview>> entry : byRepo.entrySet()) {
                List<CodeReview> list = entry.getValue();
                CodeReview first = list.get(0);
                long total = list.size();
                long completed = list.stream().filter(r -> r.getStatus() == CodeReviewStatus.COMPLETED).count();
                long failed = list.stream().filter(r -> r.getStatus() == CodeReviewStatus.FAILED).count();
                long inProgress = list.stream().filter(r -> r.getStatus() == CodeReviewStatus.IN_PROGRESS).count();
                long totalFindings = list.stream().mapToLong(r -> r.getTotalFindings() != null ? r.getTotalFindings() : 0).sum();
                Instant lastReview = list.stream().map(CodeReview::getCreatedAt).filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);

                results.add(new RepositoryReviewSummaryProjection() {
                    @Override public String getOwner() { return first.getOwner(); }
                    @Override public String getRepository() { return first.getRepository(); }
                    @Override public Long getTotalReviews() { return total; }
                    @Override public Long getCompletedReviews() { return completed; }
                    @Override public Long getFailedReviews() { return failed; }
                    @Override public Long getInProgressReviews() { return inProgress; }
                    @Override public Long getTotalFindings() { return totalFindings; }
                    @Override public Instant getLastReviewAt() { return lastReview; }
                });
            }
            return results;
        }

        private List<CodeReview> filterReviews(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            return database.values().stream()
                    .filter(r -> userId == null || (r.getUser() != null && userId.equals(r.getUser().getId())))
                    .filter(r -> owner == null || r.getOwner().equalsIgnoreCase(owner))
                    .filter(r -> repository == null || r.getRepository().equalsIgnoreCase(repository))
                    .filter(r -> fromInstant == null || (r.getCreatedAt() != null && !r.getCreatedAt().isBefore(fromInstant)))
                    .filter(r -> toInstant == null || (r.getCreatedAt() != null && !r.getCreatedAt().isAfter(toInstant)))
                    .toList();
        }
    }

    private static class InMemoryCodeReviewFindingRepository extends BaseStubJpaRepository<CodeReviewFinding, Long> implements CodeReviewFindingRepository {
        private long idGen = 1L;

        public void saveFinding(CodeReview review, ReviewFindingSeverity severity, ReviewFindingCategory category, String message) {
            CodeReviewFinding f = new CodeReviewFinding(review, "Test.java", 1, 1, severity, category, message, null);
            f.setId(idGen++);
            database.put(f.getId(), f);
        }

        @Override
        public <S extends CodeReviewFinding> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public List<CodeReviewFinding> findByCodeReviewIdOrderByFilePathAscLineNumberAsc(Long codeReviewId) { return List.of(); }
        @Override public Page<CodeReviewFinding> findByCodeReviewId(Long codeReviewId, Pageable pageable) { return Page.empty(); }

        @Override
        public List<FindingSeverityGroupProjection> countFindingsBySeverity(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReviewFinding> filtered = filterFindings(userId, owner, repository, fromInstant, toInstant);
            Map<ReviewFindingSeverity, Long> counts = new HashMap<>();
            for (CodeReviewFinding f : filtered) {
                if (f.getSeverity() != null) {
                    counts.put(f.getSeverity(), counts.getOrDefault(f.getSeverity(), 0L) + 1);
                }
            }
            return counts.entrySet().stream().map(e -> (FindingSeverityGroupProjection) new FindingSeverityGroupProjection() {
                @Override public ReviewFindingSeverity getSeverity() { return e.getKey(); }
                @Override public Long getCount() { return e.getValue(); }
            }).toList();
        }

        @Override
        public List<FindingCategoryGroupProjection> countFindingsByCategory(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReviewFinding> filtered = filterFindings(userId, owner, repository, fromInstant, toInstant);
            Map<ReviewFindingCategory, Long> counts = new HashMap<>();
            for (CodeReviewFinding f : filtered) {
                if (f.getCategory() != null) {
                    counts.put(f.getCategory(), counts.getOrDefault(f.getCategory(), 0L) + 1);
                }
            }
            return counts.entrySet().stream().map(e -> (FindingCategoryGroupProjection) new FindingCategoryGroupProjection() {
                @Override public ReviewFindingCategory getCategory() { return e.getKey(); }
                @Override public Long getCount() { return e.getValue(); }
            }).toList();
        }

        @Override
        public long countRuleBasedFindings(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            return filterFindings(userId, owner, repository, fromInstant, toInstant).stream()
                    .filter(f -> f.getMessage() != null && (
                            f.getMessage().contains("System.out/err") ||
                            f.getMessage().contains("Empty catch block detected") ||
                            f.getMessage().contains("Unresolved TODO/FIXME marker")
                    )).count();
        }

        @Override
        public long countTotalFindings(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            return filterFindings(userId, owner, repository, fromInstant, toInstant).size();
        }

        @Override
        public List<RepositoryFindingSeverityProjection> countRepositoryFindingsBySeverity(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            List<CodeReviewFinding> filtered = filterFindings(userId, owner, repository, fromInstant, toInstant);
            Map<String, Long> grouped = new HashMap<>();
            for (CodeReviewFinding f : filtered) {
                if (f.getCodeReview() != null && f.getSeverity() != null) {
                    String key = f.getCodeReview().getOwner() + "|||" + f.getCodeReview().getRepository() + "|||" + f.getSeverity().name();
                    grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
                }
            }

            return grouped.entrySet().stream().map(e -> {
                String[] parts = e.getKey().split("\\|\\|\\|");
                return (RepositoryFindingSeverityProjection) new RepositoryFindingSeverityProjection() {
                    @Override public String getOwner() { return parts[0]; }
                    @Override public String getRepository() { return parts[1]; }
                    @Override public ReviewFindingSeverity getSeverity() { return ReviewFindingSeverity.valueOf(parts[2]); }
                    @Override public Long getCount() { return e.getValue(); }
                };
            }).toList();
        }

        private List<CodeReviewFinding> filterFindings(Long userId, String owner, String repository, Instant fromInstant, Instant toInstant) {
            return database.values().stream().filter(f -> {
                CodeReview r = f.getCodeReview();
                if (r == null) return false;
                if (userId != null && (r.getUser() == null || !userId.equals(r.getUser().getId()))) return false;
                if (owner != null && !r.getOwner().equalsIgnoreCase(owner)) return false;
                if (repository != null && !r.getRepository().equalsIgnoreCase(repository)) return false;
                if (fromInstant != null && r.getCreatedAt() != null && r.getCreatedAt().isBefore(fromInstant)) return false;
                if (toInstant != null && r.getCreatedAt() != null && r.getCreatedAt().isAfter(toInstant)) return false;
                return true;
            }).toList();
        }
    }

    private static class InMemoryRepositoryRepository extends BaseStubJpaRepository<Repository, Long> implements RepositoryRepository {
        private long idGen = 1L;

        @Override
        public <S extends Repository> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public boolean existsByGithubRepositoryId(Long githubRepositoryId) { return false; }
        @Override
        public List<Repository> findByUserId(Long userId) {
            return database.values().stream().filter(r -> r.getUser() != null && userId.equals(r.getUser().getId())).toList();
        }
        @Override public Optional<Repository> findByIdAndUserId(Long id, Long userId) { return Optional.empty(); }
    }

    private static abstract class BaseStubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<T> {
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
        @Override public List<T> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public <S extends T> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public Page<T> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values()), pageable, database.size()); }
        @Override public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }

        @Override public Optional<T> findOne(Specification<T> spec) { return Optional.empty(); }
        @Override public List<T> findAll(Specification<T> spec) { return new ArrayList<>(database.values()); }
        @Override public Page<T> findAll(Specification<T> spec, Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values()), pageable, database.size()); }
        @Override public List<T> findAll(Specification<T> spec, Sort sort) { return new ArrayList<>(database.values()); }
        @Override public long count(Specification<T> spec) { return database.size(); }
        @Override public boolean exists(Specification<T> spec) { return !database.isEmpty(); }
        @Override public long delete(Specification<T> spec) { return 0; }
        @Override public <S extends T, R> R findBy(Specification<T> spec, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
