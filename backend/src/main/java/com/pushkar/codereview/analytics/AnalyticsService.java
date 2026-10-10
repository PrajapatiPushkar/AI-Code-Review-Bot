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
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final CodeReviewRepository reviewRepository;
    private final CodeReviewFindingRepository findingRepository;
    private final RepositoryRepository repositoryRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(CodeReviewRepository reviewRepository,
                            CodeReviewFindingRepository findingRepository,
                            RepositoryRepository repositoryRepository,
                            CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.repositoryRepository = repositoryRepository;
        this.currentUserService = currentUserService;
    }

    public AnalyticsOverviewResponse getOverview(String fromStr, String toStr, String repository, String owner) {
        Long targetUserId = resolveTargetUserId();
        ParsedDateRange range = parseDateRange(fromStr, toStr);
        RepoFilter repoFilter = parseRepoFilter(repository, owner);

        ReviewOverviewProjection projection = reviewRepository.getOverviewStats(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );

        long totalFindings = findingRepository.countTotalFindings(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );

        if (projection == null || projection.getTotalReviews() == null) {
            return new AnalyticsOverviewResponse(0, 0, 0, 0, totalFindings);
        }

        long totalReviews = projection.getTotalReviews() != null ? projection.getTotalReviews() : 0L;
        long completedReviews = projection.getCompletedReviews() != null ? projection.getCompletedReviews() : 0L;
        long failedReviews = projection.getFailedReviews() != null ? projection.getFailedReviews() : 0L;
        long inProgressReviews = projection.getInProgressReviews() != null ? projection.getInProgressReviews() : 0L;

        return new AnalyticsOverviewResponse(totalReviews, completedReviews, failedReviews, inProgressReviews, totalFindings);
    }

    public AnalyticsFindingsResponse getFindings(String fromStr, String toStr, String repository, String owner) {
        Long targetUserId = resolveTargetUserId();
        ParsedDateRange range = parseDateRange(fromStr, toStr);
        RepoFilter repoFilter = parseRepoFilter(repository, owner);

        long totalFindings = findingRepository.countTotalFindings(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );

        // Severity Breakdown: Initialize all severities with explicit 0
        Map<String, Long> severityMap = new LinkedHashMap<>();
        for (ReviewFindingSeverity severity : ReviewFindingSeverity.values()) {
            severityMap.put(severity.name(), 0L);
        }
        List<FindingSeverityGroupProjection> severityGroups = findingRepository.countFindingsBySeverity(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );
        if (severityGroups != null) {
            for (FindingSeverityGroupProjection g : severityGroups) {
                if (g.getSeverity() != null && g.getCount() != null) {
                    severityMap.put(g.getSeverity().name(), g.getCount());
                }
            }
        }

        // Category Breakdown: Initialize all standard categories with explicit 0
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (ReviewFindingCategory category : ReviewFindingCategory.values()) {
            categoryMap.put(category.name(), 0L);
        }
        List<FindingCategoryGroupProjection> categoryGroups = findingRepository.countFindingsByCategory(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );
        if (categoryGroups != null) {
            for (FindingCategoryGroupProjection g : categoryGroups) {
                if (g.getCategory() != null && g.getCount() != null) {
                    categoryMap.put(g.getCategory().name(), g.getCount());
                } else if (g.getCount() != null) {
                    categoryMap.put("OTHER", categoryMap.getOrDefault("OTHER", 0L) + g.getCount());
                }
            }
        }

        // Source Breakdown: AI vs RULE
        long ruleCount = findingRepository.countRuleBasedFindings(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );
        long aiCount = Math.max(0L, totalFindings - ruleCount);
        Map<String, Long> sourceMap = new LinkedHashMap<>();
        sourceMap.put("AI", aiCount);
        sourceMap.put("RULE", ruleCount);

        return new AnalyticsFindingsResponse(totalFindings, severityMap, categoryMap, sourceMap);
    }

    public List<AnalyticsTrendItemResponse> getTrends(String fromStr, String toStr, String repository, String owner) {
        Long targetUserId = resolveTargetUserId();
        ParsedDateRange range = parseDateRange(fromStr, toStr);
        RepoFilter repoFilter = parseRepoFilter(repository, owner);

        LocalDate startDate;
        LocalDate endDate;
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        if (range.fromDate() != null && range.toDate() != null) {
            startDate = range.fromDate();
            endDate = range.toDate();
        } else if (range.fromDate() != null) {
            startDate = range.fromDate();
            endDate = startDate.plusDays(30).isBefore(todayUtc) ? startDate.plusDays(30) : todayUtc;
        } else if (range.toDate() != null) {
            endDate = range.toDate();
            startDate = endDate.minusDays(14);
        } else {
            // Default period: Last 14 days up to today
            endDate = todayUtc;
            startDate = todayUtc.minusDays(13);
        }

        Instant trendFrom = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant trendTo = endDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        List<ReviewTrendRowProjection> rows = reviewRepository.getTrendRows(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                trendFrom,
                trendTo
        );

        // Group rows by Date
        Map<LocalDate, List<ReviewTrendRowProjection>> groupedByDate = new HashMap<>();
        if (rows != null) {
            for (ReviewTrendRowProjection row : rows) {
                if (row.getCreatedAt() != null) {
                    LocalDate date = row.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                    groupedByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(row);
                }
            }
        }

        // Fill all dates in range with explicit zeros for days with no activity
        List<AnalyticsTrendItemResponse> trendList = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            List<ReviewTrendRowProjection> dayRows = groupedByDate.getOrDefault(cur, Collections.emptyList());
            long dayTotal = dayRows.size();
            long dayCompleted = 0;
            long dayFailed = 0;
            long dayInProgress = 0;
            long dayFindings = 0;

            for (ReviewTrendRowProjection r : dayRows) {
                if (r.getStatus() == CodeReviewStatus.COMPLETED) {
                    dayCompleted++;
                } else if (r.getStatus() == CodeReviewStatus.FAILED) {
                    dayFailed++;
                } else if (r.getStatus() == CodeReviewStatus.IN_PROGRESS) {
                    dayInProgress++;
                }
                if (r.getTotalFindings() != null) {
                    dayFindings += r.getTotalFindings();
                }
            }

            trendList.add(new AnalyticsTrendItemResponse(
                    cur.toString(),
                    dayTotal,
                    dayCompleted,
                    dayFailed,
                    dayInProgress,
                    dayFindings
            ));

            cur = cur.plusDays(1);
        }

        return trendList;
    }

    public List<RepositoryHealthSummaryResponse> getRepositorySummaries(String fromStr, String toStr, String repository, String owner) {
        Long targetUserId = resolveTargetUserId();
        ParsedDateRange range = parseDateRange(fromStr, toStr);
        RepoFilter repoFilter = parseRepoFilter(repository, owner);

        // 1. Fetch review aggregations grouped by repository
        List<RepositoryReviewSummaryProjection> reviewSummaries = reviewRepository.getRepositoryReviewSummaries(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );

        // 2. Fetch finding severity counts grouped by repository
        List<RepositoryFindingSeverityProjection> severitySummaries = findingRepository.countRepositoryFindingsBySeverity(
                targetUserId,
                repoFilter.owner(),
                repoFilter.repo(),
                range.fromInstant(),
                range.toInstant()
        );

        Map<String, Map<ReviewFindingSeverity, Long>> severityByRepo = new HashMap<>();
        if (severitySummaries != null) {
            for (RepositoryFindingSeverityProjection s : severitySummaries) {
                if (s.getOwner() != null && s.getRepository() != null && s.getSeverity() != null && s.getCount() != null) {
                    String key = (s.getOwner() + "/" + s.getRepository()).toLowerCase();
                    severityByRepo.computeIfAbsent(key, k -> new HashMap<>()).put(s.getSeverity(), s.getCount());
                }
            }
        }

        Map<String, RepositoryHealthSummaryResponse> responseMap = new LinkedHashMap<>();

        // Process repositories with reviews
        if (reviewSummaries != null) {
            for (RepositoryReviewSummaryProjection r : reviewSummaries) {
                String repoKey = (r.getOwner() + "/" + r.getRepository()).toLowerCase();
                String repoFull = r.getOwner() + "/" + r.getRepository();

                long totalReviews = r.getTotalReviews() != null ? r.getTotalReviews() : 0L;
                long completedReviews = r.getCompletedReviews() != null ? r.getCompletedReviews() : 0L;
                long failedReviews = r.getFailedReviews() != null ? r.getFailedReviews() : 0L;
                long inProgressReviews = r.getInProgressReviews() != null ? r.getInProgressReviews() : 0L;
                long totalFindings = r.getTotalFindings() != null ? r.getTotalFindings() : 0L;

                Map<ReviewFindingSeverity, Long> sevMap = severityByRepo.getOrDefault(repoKey, Collections.emptyMap());
                long critical = sevMap.getOrDefault(ReviewFindingSeverity.CRITICAL, 0L);
                long high = sevMap.getOrDefault(ReviewFindingSeverity.HIGH, 0L);
                long medium = sevMap.getOrDefault(ReviewFindingSeverity.MEDIUM, 0L);
                long low = sevMap.getOrDefault(ReviewFindingSeverity.LOW, 0L);
                long info = sevMap.getOrDefault(ReviewFindingSeverity.INFO, 0L);

                RepositoryHealthStatus status;
                if (totalReviews == 0) {
                    status = RepositoryHealthStatus.NO_REVIEWS;
                } else if (totalFindings == 0) {
                    status = RepositoryHealthStatus.NO_FINDINGS_RECORDED;
                } else {
                    status = RepositoryHealthStatus.COMPLETED_WITH_FINDINGS;
                }

                responseMap.put(repoKey, new RepositoryHealthSummaryResponse(
                        repoFull,
                        r.getOwner(),
                        r.getRepository(),
                        totalReviews,
                        completedReviews,
                        failedReviews,
                        inProgressReviews,
                        totalFindings,
                        critical,
                        high,
                        medium,
                        low,
                        info,
                        r.getLastReviewAt(),
                        status
                ));
            }
        }

        // 3. Include registered repositories that have zero reviews
        if (repositoryRepository != null) {
            List<Repository> registeredList;
            if (targetUserId != null) {
                registeredList = repositoryRepository.findByUserId(targetUserId);
            } else {
                registeredList = repositoryRepository.findAll();
            }

            if (registeredList != null) {
                for (Repository repo : registeredList) {
                    String fullName = repo.getFullName();
                    if (fullName == null || fullName.isBlank()) {
                        continue;
                    }
                    String key = fullName.toLowerCase();
                    if (!responseMap.containsKey(key)) {
                        String[] parts = fullName.split("/", 2);
                        String repoOwner = parts.length > 1 ? parts[0] : (repo.getUser() != null ? repo.getUser().getUsername() : "unknown");
                        String repoName = parts.length > 1 ? parts[1] : repo.getName();

                        // Apply repository filter if present
                        if (repoFilter.owner() != null && !repoFilter.owner().equalsIgnoreCase(repoOwner)) {
                            continue;
                        }
                        if (repoFilter.repo() != null && !repoFilter.repo().equalsIgnoreCase(repoName)) {
                            continue;
                        }

                        responseMap.put(key, new RepositoryHealthSummaryResponse(
                                fullName,
                                repoOwner,
                                repoName,
                                0L, 0L, 0L, 0L, 0L,
                                0L, 0L, 0L, 0L, 0L,
                                null,
                                RepositoryHealthStatus.NO_REVIEWS
                        ));
                    }
                }
            }
        }

        List<RepositoryHealthSummaryResponse> list = new ArrayList<>(responseMap.values());
        list.sort((a, b) -> {
            int cmp = Long.compare(b.getTotalReviews(), a.getTotalReviews());
            if (cmp != 0) return cmp;
            int findCmp = Long.compare(b.getTotalFindings(), a.getTotalFindings());
            if (findCmp != 0) return findCmp;
            return a.getRepository().compareToIgnoreCase(b.getRepository());
        });

        return list;
    }

    private Long resolveTargetUserId() {
        if (currentUserService == null || !currentUserService.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated to access analytics");
        }
        if (currentUserService.hasRole("ADMIN")) {
            return null;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("Authenticated user identifier could not be determined");
        }
        return userId;
    }

    private ParsedDateRange parseDateRange(String fromStr, String toStr) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        Instant fromInstant = null;
        Instant toInstant = null;

        if (fromStr != null && !fromStr.isBlank()) {
            String trimmed = fromStr.trim();
            try {
                if (trimmed.contains("T")) {
                    fromInstant = Instant.parse(trimmed);
                    fromDate = fromInstant.atZone(ZoneOffset.UTC).toLocalDate();
                } else {
                    fromDate = LocalDate.parse(trimmed);
                    fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format for 'from': " + fromStr + ". Expected YYYY-MM-DD or ISO-8601 UTC timestamp.");
            }
        }

        if (toStr != null && !toStr.isBlank()) {
            String trimmed = toStr.trim();
            try {
                if (trimmed.contains("T")) {
                    toInstant = Instant.parse(trimmed);
                    toDate = toInstant.atZone(ZoneOffset.UTC).toLocalDate();
                } else {
                    toDate = LocalDate.parse(trimmed);
                    // Inclusive end-of-day
                    toInstant = toDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format for 'to': " + toStr + ". Expected YYYY-MM-DD or ISO-8601 UTC timestamp.");
            }
        }

        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new IllegalArgumentException("Invalid date range: 'from' (" + fromStr + ") must not be after 'to' (" + toStr + ")");
        }

        return new ParsedDateRange(fromInstant, toInstant, fromDate, toDate);
    }

    private RepoFilter parseRepoFilter(String repository, String owner) {
        String resolvedOwner = (owner != null && !owner.isBlank()) ? owner.trim() : null;
        String resolvedRepo = null;

        if (repository != null && !repository.isBlank()) {
            String trimmed = repository.trim();
            if (trimmed.contains("/")) {
                String[] parts = trimmed.split("/", 2);
                resolvedOwner = parts[0].trim();
                resolvedRepo = parts[1].trim();
            } else {
                resolvedRepo = trimmed;
            }
        }

        return new RepoFilter(resolvedOwner, resolvedRepo);
    }

    private record ParsedDateRange(Instant fromInstant, Instant toInstant, LocalDate fromDate, LocalDate toDate) {}
    private record RepoFilter(String owner, String repo) {}
}
