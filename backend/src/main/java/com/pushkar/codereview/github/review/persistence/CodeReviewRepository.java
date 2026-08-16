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
}
