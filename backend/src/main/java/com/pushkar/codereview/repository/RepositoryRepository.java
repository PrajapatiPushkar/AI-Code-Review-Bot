package com.pushkar.codereview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<com.pushkar.codereview.repository.Repository, Long> {

    boolean existsByGithubRepositoryId(Long githubRepositoryId);

    List<com.pushkar.codereview.repository.Repository> findByUserId(Long userId);

    Optional<com.pushkar.codereview.repository.Repository> findByIdAndUserId(Long id, Long userId);
}
