package com.pushkar.codereview.auth;

import com.pushkar.codereview.auth.dto.RegisterRequest;
import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import com.pushkar.codereview.user.dto.UserResponse;
import com.pushkar.codereview.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRegistrationServiceTest {

    private StubUserRepository stubUserRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private AuthRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        stubUserRepository = new StubUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        userMapper = new UserMapper();
        registrationService = new AuthRegistrationService(stubUserRepository, passwordEncoder, userMapper);
    }

    @Test
    void testValidRegistration_Success() {
        RegisterRequest request = new RegisterRequest("newuser@example.com", "strongPass123", "newuser");

        UserResponse response = registrationService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");

        User savedEntity = stubUserRepository.findByEmail("newuser@example.com").orElse(null);
        assertThat(savedEntity).isNotNull();
        assertThat(savedEntity.getPasswordHash()).isNotEqualTo("strongPass123");
        assertThat(passwordEncoder.matches("strongPass123", savedEntity.getPasswordHash())).isTrue();
        assertThat(savedEntity.getEnabled()).isTrue();
    }

    @Test
    void testRegistration_DefaultRoleIsUser_AndDerivedUsername() {
        RegisterRequest request = new RegisterRequest("john.doe@example.com", "myPass123");

        UserResponse response = registrationService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getUsername()).isEqualTo("john.doe");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void testDuplicateEmail_Rejected() {
        User existing = new User("existingUser", "duplicate@example.com", "$2a$10$hash", "USER");
        stubUserRepository.save(existing);

        RegisterRequest request = new RegisterRequest("duplicate@example.com", "pass12345");

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User with email 'duplicate@example.com' already exists");
    }

    @Test
    void testDuplicateUsername_Rejected() {
        User existing = new User("takenUsername", "user1@example.com", "$2a$10$hash", "USER");
        stubUserRepository.save(existing);

        RegisterRequest request = new RegisterRequest("user2@example.com", "pass12345", "takenUsername");

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User with username 'takenUsername' already exists");
    }

    @Test
    void testInvalidEmail_Rejected() {
        RegisterRequest nullEmail = new RegisterRequest(null, "pass12345");
        RegisterRequest blankEmail = new RegisterRequest("   ", "pass12345");

        assertThatThrownBy(() -> registrationService.register(nullEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be blank");

        assertThatThrownBy(() -> registrationService.register(blankEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be blank");
    }

    @Test
    void testInvalidPassword_Rejected() {
        RegisterRequest nullPass = new RegisterRequest("user@example.com", null);
        RegisterRequest shortPass = new RegisterRequest("user@example.com", "12345");

        assertThatThrownBy(() -> registrationService.register(nullPass))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 6 characters long");

        assertThatThrownBy(() -> registrationService.register(shortPass))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 6 characters long");
    }

    // --- Stub Repository ---

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
