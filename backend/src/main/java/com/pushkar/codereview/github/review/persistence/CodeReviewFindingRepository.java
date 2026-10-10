package com.pushkar.codereview.github.review.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewFindingRepository extends JpaRepository<CodeReviewFinding, Long> {

    List<CodeReviewFinding> findByCodeReviewIdOrderByFilePathAscLineNumberAsc(Long codeReviewId);

    Page<CodeReviewFinding> findByCodeReviewId(Long codeReviewId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT f.severity as severity, COUNT(f) as count " +
            "FROM CodeReviewFinding f WHERE " +
            "(:userId IS NULL OR (f.codeReview.user IS NOT NULL AND f.codeReview.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(f.codeReview.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(f.codeReview.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR f.codeReview.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR f.codeReview.createdAt <= :toInstant) " +
            "GROUP BY f.severity")
    List<com.pushkar.codereview.analytics.projection.FindingSeverityGroupProjection> countFindingsBySeverity(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT f.category as category, COUNT(f) as count " +
            "FROM CodeReviewFinding f WHERE " +
            "(:userId IS NULL OR (f.codeReview.user IS NOT NULL AND f.codeReview.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(f.codeReview.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(f.codeReview.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR f.codeReview.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR f.codeReview.createdAt <= :toInstant) " +
            "GROUP BY f.category")
    List<com.pushkar.codereview.analytics.projection.FindingCategoryGroupProjection> countFindingsByCategory(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(f) " +
            "FROM CodeReviewFinding f WHERE " +
            "(:userId IS NULL OR (f.codeReview.user IS NOT NULL AND f.codeReview.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(f.codeReview.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(f.codeReview.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR f.codeReview.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR f.codeReview.createdAt <= :toInstant) AND (" +
            "f.message LIKE '%System.out/err%' OR " +
            "f.message LIKE '%Empty catch block detected%' OR " +
            "f.message LIKE '%Unresolved TODO/FIXME marker%')")
    long countRuleBasedFindings(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(f) " +
            "FROM CodeReviewFinding f WHERE " +
            "(:userId IS NULL OR (f.codeReview.user IS NOT NULL AND f.codeReview.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(f.codeReview.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(f.codeReview.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR f.codeReview.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR f.codeReview.createdAt <= :toInstant)")
    long countTotalFindings(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "f.codeReview.owner as owner, " +
            "f.codeReview.repository as repository, " +
            "f.severity as severity, " +
            "COUNT(f) as count " +
            "FROM CodeReviewFinding f WHERE " +
            "(:userId IS NULL OR (f.codeReview.user IS NOT NULL AND f.codeReview.user.id = :userId)) AND " +
            "(:owner IS NULL OR LOWER(f.codeReview.owner) = LOWER(:owner)) AND " +
            "(:repository IS NULL OR LOWER(f.codeReview.repository) = LOWER(:repository)) AND " +
            "(:fromInstant IS NULL OR f.codeReview.createdAt >= :fromInstant) AND " +
            "(:toInstant IS NULL OR f.codeReview.createdAt <= :toInstant) " +
            "GROUP BY f.codeReview.owner, f.codeReview.repository, f.severity")
    List<com.pushkar.codereview.analytics.projection.RepositoryFindingSeverityProjection> countRepositoryFindingsBySeverity(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("owner") String owner,
            @org.springframework.data.repository.query.Param("repository") String repository,
            @org.springframework.data.repository.query.Param("fromInstant") java.time.Instant fromInstant,
            @org.springframework.data.repository.query.Param("toInstant") java.time.Instant toInstant
    );
}
