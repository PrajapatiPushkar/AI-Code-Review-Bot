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
}
