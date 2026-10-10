package com.pushkar.codereview.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryReviewPolicyRepository extends JpaRepository<RepositoryReviewPolicy, Long> {

    Optional<RepositoryReviewPolicy> findByRepositoryId(Long repositoryId);

    boolean existsByRepositoryId(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}
