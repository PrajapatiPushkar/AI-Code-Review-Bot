package com.pushkar.codereview.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByGithubId(Long githubId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByGithubId(Long githubId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);
}
