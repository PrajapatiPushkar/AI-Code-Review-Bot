package com.pushkar.codereview.security;

import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomUserDetailsServiceTest {

    private StubUserRepository stubRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        stubRepository = new StubUserRepository();
        userDetailsService = new CustomUserDetailsService(stubRepository);
    }

    @Test
    void testExistingUserLoadsSuccessfully_UserRoleMappedToRoleUser() {
        User user = new User("octocat", "octocat@github.com", "$2a$10$encodedHash", "USER");
        user.setId(1L);
        stubRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("octocat@github.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("octocat@github.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedHash");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void testAdminRoleMappedToRoleAdmin() {
        User admin = new User("adminUser", "admin@example.com", "$2a$10$encodedHash", "ADMIN");
        admin.setId(2L);
        stubRepository.save(admin);

        UserDetails userDetails = userDetailsService.loadUserByUsername("adminUser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testMissingUserThrowsUsernameNotFoundException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username or email: unknown@example.com");
    }

    @Test
    void testDisabledUserIsRepresentedCorrectly() {
        User disabledUser = new User("disabledOne", "disabled@example.com", "$2a$10$encodedHash", "USER");
        disabledUser.setId(3L);
        disabledUser.setEnabled(false);
        stubRepository.save(disabledUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
    }

    // --- Helper Stub ---

    private static class StubUserRepository extends StubJpaRepository<User, Long> implements UserRepository {
        @Override
        public boolean existsByGithubId(Long githubId) { return database.values().stream().anyMatch(u -> githubId != null && githubId.equals(u.getGithubId())); }
        @Override
        public boolean existsByUsername(String username) { return database.values().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username)); }
        @Override
        public boolean existsByEmail(String email) { return database.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email)); }

        @Override
        public Optional<User> findByGithubId(Long githubId) { return database.values().stream().filter(u -> githubId != null && githubId.equals(u.getGithubId())).findFirst(); }
        @Override
        public Optional<User> findByUsername(String username) { return database.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst(); }
        @Override
        public Optional<User> findByEmail(String email) { return database.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst(); }
        @Override
        public Optional<User> findByUsernameOrEmail(String username, String email) {
            return database.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username) || u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public <S extends User> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId((long) (database.size() + 1));
            }
            database.put(entity.getId(), entity);
            return entity;
        }
    }

    private static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID> {
        protected final Map<ID, T> database = new HashMap<>();

        @Override public Optional<T> findById(ID id) { return Optional.ofNullable(database.get(id)); }
        @Override public List<T> findAll() { return List.copyOf(database.values()); }
        @Override public List<T> findAllById(Iterable<ID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(ID id) { database.remove(id); }
        @Override public void delete(T entity) { }
        @Override public void deleteAllById(Iterable<? extends ID> ids) { }
        @Override public void deleteAll(Iterable<? extends T> entities) { }
        @Override public void deleteAll() { database.clear(); }
        @Override public <S extends T> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(ID id) { return database.containsKey(id); }
        @Override public void flush() { }
        @Override public <S extends T> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<T> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<ID> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public T getOne(ID id) { return database.get(id); }
        @Override public T getById(ID id) { return database.get(id); }
        @Override public T getReferenceById(ID id) { return database.get(id); }
        @Override public List<T> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
