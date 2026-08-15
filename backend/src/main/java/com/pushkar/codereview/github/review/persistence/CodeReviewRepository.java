package com.pushkar.codereview.github.review.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    List<CodeReview> findByOwnerAndRepositoryAndPullRequestNumber(String owner, String repository, Integer pullRequestNumber);

    List<CodeReview> findByOwnerAndRepositoryOrderByCreatedAtDesc(String owner, String repository);

    List<CodeReview> findAllByOrderByCreatedAtDesc();
}
