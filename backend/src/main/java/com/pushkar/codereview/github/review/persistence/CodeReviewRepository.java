package com.pushkar.codereview.github.review.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long>, JpaSpecificationExecutor<CodeReview> {

    List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber);

    List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository);

    List<CodeReview> findAllByOrderByCreatedAtDesc();

    List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<CodeReview> findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(Long userId, String owner, String repository);

    List<CodeReview> findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(Long userId, String owner, String repository, Integer pullRequestNumber);

    Optional<CodeReview> findByIdAndUserId(Long id, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM CodeReview r WHERE " +
           "(:userId IS NULL OR (r.user IS NOT NULL AND r.user.id = :userId)) AND " +
           "r.installationId = :installationId AND " +
           "LOWER(r.owner) = LOWER(:owner) AND " +
           "LOWER(r.repository) = LOWER(:repository) AND " +
           "r.pullRequestNumber = :pullRequestNumber AND " +
           "(:commitSha IS NULL OR r.commitSha = :commitSha) AND " +
           "r.status IN :statuses " +
           "ORDER BY r.id DESC")
    List<CodeReview> findDuplicateReviews(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("installationId") Long installationId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("pullRequestNumber") Integer pullRequestNumber,
            @org.springframework.data.repository.query.Param("commitSha") String commitSha,
            @org.springframework.data.repository.query.Param("statuses") List<CodeReviewStatus> statuses
    );

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(r) as totalReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.COMPLETED THEN 1L ELSE 0L END), 0L) as completedReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.FAILED THEN 1L ELSE 0L END), 0L) as failedReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.IN_PROGRESS THEN 1L ELSE 0L END), 0L) as inProgressReviews, " +
            "COALESCE(SUM(r.totalFindings), 0L) as totalFindings " +
            "FROM CodeReview r WHERE " +
            "(:userId IS NULL OR (r.user IS NOT NULL AND r.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(r.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(r.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR r.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR r.createdAt <= :toInstant)")
    com.pushkar.codereview.analytics.projection.ReviewOverviewProjection getOverviewStats(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT r.createdAt as createdAt, r.status as status, r.totalFindings as totalFindings " +
            "FROM CodeReview r WHERE " +
            "(:userId IS NULL OR (r.user IS NOT NULL AND r.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(r.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(r.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR r.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR r.createdAt <= :toInstant) " +
            "ORDER BY r.createdAt ASC")
    List<com.pushkar.codereview.analytics.projection.ReviewTrendRowProjection> getTrendRows(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "r.owner as owner, " +
            "r.repository as repository, " +
            "COUNT(r) as totalReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.COMPLETED THEN 1L ELSE 0L END), 0L) as completedReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.FAILED THEN 1L ELSE 0L END), 0L) as failedReviews, " +
            "COALESCE(SUM(CASE WHEN r.status = com.pushkar.codereview.github.review.persistence.CodeReviewStatus.IN_PROGRESS THEN 1L ELSE 0L END), 0L) as inProgressReviews, " +
            "COALESCE(SUM(r.totalFindings), 0L) as totalFindings, " +
            "MAX(r.createdAt) as lastReviewAt " +
            "FROM CodeReview r WHERE " +
            "(:userId IS NULL OR (r.user IS NOT NULL AND r.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(r.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(r.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR r.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR r.createdAt <= :toInstant) " +
            "GROUP BY r.owner, r.repository " +
            "ORDER BY COUNT(r) DESC, r.repository ASC")
    List<com.pushkar.codereview.analytics.projection.RepositoryReviewSummaryProjection> getRepositoryReviewSummaries(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );
}
