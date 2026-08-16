package com.pushkar.codereview.github;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.GithubInstallationTokenClient;
import com.pushkar.codereview.github.client.GithubInstallationVerificationClient;
import com.pushkar.codereview.github.client.dto.GithubInstallationDetailsResponse;
import com.pushkar.codereview.github.client.dto.GithubInstallationTokenResponse;
import com.pushkar.codereview.github.dto.GithubInstallationRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.github.mapper.GithubInstallationMapper;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubInstallationServiceTest {

    private StubGithubInstallationRepository installationRepository;
    private StubUserRepository userRepository;
    private StubCurrentUserService currentUserService;
    private StubTokenClient tokenClient;
    private StubVerificationClient verificationClient;
    private GithubInstallationService installationService;

    private User user1;
    private User user2;
    private User devUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        installationRepository = new StubGithubInstallationRepository();
        userRepository = new StubUserRepository();
        currentUserService = new StubCurrentUserService();
        tokenClient = new StubTokenClient();
        verificationClient = new StubVerificationClient();

        GithubInstallationMapper mapper = new GithubInstallationMapper();
        installationService = new GithubInstallationService(
                installationRepository, userRepository, mapper, currentUserService, tokenClient, verificationClient
        );

        user1 = new User("user1", "user1@example.com", "hash", "USER");
        user1.setId(10L);
        userRepository.save(user1);

        user2 = new User("user2", "user2@example.com", "hash", "USER");
        user2.setId(20L);
        userRepository.save(user2);

        devUser = new User("devuser", "dev@example.com", "hash", "DEVELOPER");
        devUser.setId(30L);
        userRepository.save(devUser);

        adminUser = new User("adminuser", "admin@example.com", "hash", "ADMIN");
        adminUser.setId(99L);
        userRepository.save(adminUser);
    }

    @AfterEach
    void tearDown() {
        currentUserService.clear();
        tokenClient.clear();
        verificationClient.clear();
    }

    @Test
    void testRegisterInstallation_VerificationSuccess_SetsVerifiedAndTimestamp() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);
        verificationClient.setDetails(123456L, "octocat", "User");

        GithubInstallationRequest request = new GithubInstallationRequest(123456L, "octocat", "User");
        GithubInstallationResponse response = installationService.registerInstallation(request);

        assertThat(response).isNotNull();
        assertThat(response.getGithubInstallationId()).isEqualTo(123456L);
        assertThat(response.getGithubAccountLogin()).isEqualTo("octocat");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.getVerifiedAt()).isNotNull();
        assertThat(tokenClient.isTokenRequested()).isTrue();
    }

    @Test
    void testRegisterInstallation_AccountMismatch_ThrowsVerificationException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);
        verificationClient.setDetails(123456L, "real-github-login", "User");

        GithubInstallationRequest request = new GithubInstallationRequest(123456L, "fake-login", "User");

        assertThatThrownBy(() -> installationService.registerInstallation(request))
                .isInstanceOf(GithubInstallationVerificationException.class)
                .hasMessageContaining("mismatch");

        assertThat(installationRepository.findByGithubInstallationId(123456L)).isEmpty();
    }

    @Test
    void testRegisterInstallation_GithubNotFound_ThrowsResourceNotFoundException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);
        tokenClient.setNotFoundId(999999L);

        GithubInstallationRequest request = new GithubInstallationRequest(999999L, "octocat", "User");

        assertThatThrownBy(() -> installationService.registerInstallation(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found on GitHub");
    }

    @Test
    void testRegisterInstallation_GithubUnauthorized_ThrowsGithubApiException() {
        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);
        tokenClient.setUnauthorizedId(888888L);

        GithubInstallationRequest request = new GithubInstallationRequest(888888L, "octocat", "User");

        assertThatThrownBy(() -> installationService.registerInstallation(request))
                .isInstanceOf(GithubApiException.class)
                .hasMessageContaining("failed");
    }

    @Test
    void testRegisterInstallation_SameUserReRegistration_VerifiedReturnsExistingIdempotently() {
        GithubInstallation i1 = createInstallation(1L, user1, 123456L, "octocat", "User");
        i1.setVerified(true);
        i1.setVerifiedAt(Instant.now());
        installationRepository.save(i1);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);

        GithubInstallationRequest request = new GithubInstallationRequest(123456L, "octocat", "User");
        GithubInstallationResponse response = installationService.registerInstallation(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.isVerified()).isTrue();
    }

    @Test
    void testRegisterInstallation_OwnedByAnotherUser_ThrowsDuplicateResourceException() {
        GithubInstallation i1 = createInstallation(1L, user1, 123456L, "octocat", "User");
        installationRepository.save(i1);

        currentUserService.setContext(user2.getId(), "user2@example.com", "USER", user2);

        GithubInstallationRequest request = new GithubInstallationRequest(123456L, "octocat", "User");

        assertThatThrownBy(() -> installationService.registerInstallation(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered to another user");
    }

    @Test
    void testGetInstallationsForCurrentUser_UserSeesOnlyOwnInstallations() {
        GithubInstallation i1 = createInstallation(1L, user1, 10001L, "octocat", "User");
        GithubInstallation i2 = createInstallation(2L, user2, 10002L, "anothercat", "User");
        installationRepository.save(i1);
        installationRepository.save(i2);

        currentUserService.setContext(user1.getId(), "user1@example.com", "USER", user1);

        List<GithubInstallationResponse> responses = installationService.getInstallationsForCurrentUser();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void testGetInstallationById_NonOwnerAccess_ThrowsAccessDeniedException() {
        GithubInstallation i1 = createInstallation(1L, user1, 10001L, "octocat", "User");
        installationRepository.save(i1);

        currentUserService.setContext(user2.getId(), "user2@example.com", "USER", user2);

        assertThatThrownBy(() -> installationService.getInstallationById(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private GithubInstallation createInstallation(Long id, User user, Long githubInstId, String login, String type) {
        GithubInstallation installation = new GithubInstallation(user, githubInstId, login, type);
        installation.setId(id);
        return installation;
    }

    // --- Helper Stubs ---

    private static class StubTokenClient extends GithubInstallationTokenClient {
        private boolean tokenRequested = false;
        private Long notFoundId;
        private Long unauthorizedId;

        public StubTokenClient() { super(null, null); }

        public boolean isTokenRequested() { return tokenRequested; }
        public void setNotFoundId(Long id) { this.notFoundId = id; }
        public void setUnauthorizedId(Long id) { this.unauthorizedId = id; }
        public void clear() { tokenRequested = false; notFoundId = null; unauthorizedId = null; }

        @Override
        public GithubInstallationTokenResponse requestInstallationToken(Long installationId) {
            tokenRequested = true;
            if (installationId.equals(notFoundId)) {
                throw new ResourceNotFoundException("GitHub installation not found on GitHub with ID: " + installationId);
            }
            if (installationId.equals(unauthorizedId)) {
                throw new GithubApiException("GitHub API authentication failed (HTTP 401)", 401);
            }
            return new GithubInstallationTokenResponse("mock-token", Instant.now().plusSeconds(3600));
        }
    }

    private static class StubVerificationClient extends GithubInstallationVerificationClient {
        private final Map<Long, GithubInstallationDetailsResponse> map = new HashMap<>();

        public StubVerificationClient() { super(null, null); }

        public void setDetails(Long id, String login, String type) {
            map.put(id, new GithubInstallationDetailsResponse(id, login, type));
        }

        public void clear() { map.clear(); }

        @Override
        public GithubInstallationDetailsResponse getInstallationDetails(Long installationId) {
            GithubInstallationDetailsResponse res = map.get(installationId);
            if (res != null) return res;
            return new GithubInstallationDetailsResponse(installationId, "octocat", "User");
        }
    }

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;
        private User user;

        public StubCurrentUserService() { super(null); }

        public void setContext(Long userId, String username, String role, User user) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.user = user;
        }

        public void clear() {
            this.userId = null;
            this.username = null;
            this.role = null;
            this.user = null;
        }

        @Override public boolean isAuthenticated() { return username != null; }
        @Override public String getCurrentUsername() { return username; }
        @Override public Long getCurrentUserId() { return userId; }
        @Override public User getCurrentUser() {
            if (user == null) {
                throw new ResourceNotFoundException("No authenticated user");
            }
            return user;
        }
        @Override
        public boolean hasRole(String roleName) {
            if (role == null) return false;
            String normalized = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            return role.equalsIgnoreCase(normalized);
        }
    }

    private static class StubGithubInstallationRepository extends StubJpaRepository<GithubInstallation, Long> implements GithubInstallationRepository {
        private long seq = 1L;

        @Override
        public <S extends GithubInstallation> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public boolean existsByGithubInstallationId(Long githubInstallationId) {
            return database.values().stream().anyMatch(i -> githubInstallationId != null && githubInstallationId.equals(i.getGithubInstallationId()));
        }

        @Override
        public List<GithubInstallation> findByUserId(Long userId) {
            return database.values().stream().filter(i -> i.getUser() != null && userId.equals(i.getUser().getId())).toList();
        }

        @Override
        public Optional<GithubInstallation> findByGithubInstallationId(Long githubInstallationId) {
            return database.values().stream().filter(i -> githubInstallationId != null && githubInstallationId.equals(i.getGithubInstallationId())).findFirst();
        }
    }

    private static class StubUserRepository extends StubJpaRepository<User, Long> implements UserRepository {
        @Override public boolean existsByGithubId(Long githubId) { return false; }
        @Override public boolean existsByUsername(String username) { return false; }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public Optional<User> findByGithubId(Long githubId) { return Optional.empty(); }
        @Override public Optional<User> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
        @Override public Optional<User> findByUsernameOrEmail(String username, String email) { return Optional.empty(); }

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
        @Override public List<T> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<T> findAllById(Iterable<ID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(ID id) { database.remove(id); }
        @Override public void delete(T entity) {
            if (entity != null) {
                database.values().removeIf(e -> e.equals(entity));
            }
        }
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
