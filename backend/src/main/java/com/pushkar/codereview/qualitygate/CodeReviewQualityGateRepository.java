package com.pushkar.codereview.qualitygate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeReviewQualityGateRepository extends JpaRepository<CodeReviewQualityGate, Long> {

    Optional<CodeReviewQualityGate> findByCodeReviewId(Long codeReviewId);

    boolean existsByCodeReviewId(Long codeReviewId);
}
