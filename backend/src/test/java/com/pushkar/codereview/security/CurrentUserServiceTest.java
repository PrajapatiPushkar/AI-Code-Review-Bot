package com.pushkar.codereview.security;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    private StubUserRepository userRepository;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userRepository = new StubUserRepository();
        currentUserService = new CurrentUserService(userRepository);

        User testUser = new User("testuser", "test@example.com", "hash", "USER");
        testUser.setId(42L);
        userRepository.save(testUser);

        User adminUser = new User("adminuser", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);
        userRepository.save(adminUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUnauthenticated_ReturnsNullAndFalse() {
        assertThat(currentUserService.isAuthenticated()).isFalse();
        assertThat(currentUserService.getCurrentUsername()).isNull();
        assertThat(currentUserService.getCurrentUserId()).isNull();
        assertThat(currentUserService.findCurrentUser()).isEmpty();
        assertThat(currentUserService.hasRole("USER")).isFalse();

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void testAuthenticatedUser_ReturnsUserDetailsAndRoles() {
        setAuthentication("test@example.com", "ROLE_USER");

        assertThat(currentUserService.isAuthenticated()).isTrue();
        assertThat(currentUserService.getCurrentUsername()).isEqualTo("test@example.com");
        assertThat(currentUserService.getCurrentUserId()).isEqualTo(42L);

        User currentUser = currentUserService.getCurrentUser();
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getId()).isEqualTo(42L);
        assertThat(currentUser.getEmail()).isEqualTo("test@example.com");

        assertThat(currentUserService.hasRole("USER")).isTrue();
        assertThat(currentUserService.hasRole("ROLE_USER")).isTrue();
        assertThat(currentUserService.hasRole("ADMIN")).isFalse();
    }

    @Test
    void testAuthenticatedAdmin_HasAdminRole() {
        setAuthentication("admin@example.com", "ROLE_ADMIN");

        assertThat(currentUserService.isAuthenticated()).isTrue();
        assertThat(currentUserService.getCurrentUsername()).isEqualTo("admin@example.com");
        assertThat(currentUserService.getCurrentUserId()).isEqualTo(99L);

        assertThat(currentUserService.hasRole("ADMIN")).isTrue();
        assertThat(currentUserService.hasRole("USER")).isFalse();
    }

    @Test
    void testMissingUserInRepository_ThrowsResourceNotFoundException() {
        setAuthentication("nonexistent@example.com", "ROLE_USER");

        assertThat(currentUserService.isAuthenticated()).isTrue();
        assertThat(currentUserService.getCurrentUsername()).isEqualTo("nonexistent@example.com");
        assertThat(currentUserService.findCurrentUser()).isEmpty();

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user record not found");
    }

    private void setAuthentication(String username, String role) {
        var authorities = List.of(new SimpleGrantedAuthority(role));
        var authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private static class StubUserRepository implements UserRepository {
        private final Map<Long, User> database = new HashMap<>();

        @Override
        public Optional<User> findByEmail(String email) {
            return database.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return database.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override public boolean existsByGithubId(Long githubId) { return false; }
        @Override public boolean existsByUsername(String username) { return false; }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public Optional<User> findByGithubId(Long githubId) { return Optional.empty(); }
        @Override public Optional<User> findByUsernameOrEmail(String username, String email) { return Optional.empty(); }

        @Override
        public <S extends User> S save(S entity) {
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(database.get(id)); }
        @Override public boolean existsById(Long id) { return database.containsKey(id); }
        @Override public List<User> findAll() { return List.copyOf(database.values()); }
        @Override public List<User> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long id) { database.remove(id); }
        @Override public void delete(User entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { }
        @Override public void deleteAll(Iterable<? extends User> entities) { }
        @Override public void deleteAll() { database.clear(); }
        @Override public void flush() { }
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public User getOne(Long id) { return database.get(id); }
        @Override public User getById(Long id) { return database.get(id); }
        @Override public User getReferenceById(Long id) { return database.get(id); }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
    }
}
