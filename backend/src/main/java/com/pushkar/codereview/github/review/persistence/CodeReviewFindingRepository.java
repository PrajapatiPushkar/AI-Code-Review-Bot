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
}
