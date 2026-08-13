package com.pushkar.codereview.github;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubInstallationRepository extends JpaRepository<GithubInstallation, Long> {

    boolean existsByGithubInstallationId(Long githubInstallationId);

    List<GithubInstallation> findByUserId(Long userId);

    Optional<GithubInstallation> findByGithubInstallationId(Long githubInstallationId);
}
