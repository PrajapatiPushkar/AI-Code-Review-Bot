package com.pushkar.codereview.repository;

import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.repository.dto.RepositoryCreateRequest;
import com.pushkar.codereview.repository.dto.RepositoryResponse;
import com.pushkar.codereview.repository.mapper.RepositoryMapper;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryServiceTest {

    private InMemoryRepositoryRepository repositoryRepository;
    private InMemoryUserRepository userRepository;
    private RepositoryMapper repositoryMapper;
    private StubCurrentUserService currentUserService;
    private RepositoryService service;

    private User userA;
    private User userB;
    private User adminUser;
    private Repository repoA;
    private Repository repoB;

    @BeforeEach
    void setUp() {
        repositoryRepository = new InMemoryRepositoryRepository();
        userRepository = new InMemoryUserRepository();
        repositoryMapper = new RepositoryMapper();
        currentUserService = new StubCurrentUserService();

        service = new RepositoryService(repositoryRepository, userRepository, repositoryMapper, currentUserService);

        userA = new User("usera", "userA@example.com", "passwordHash", "USER");
        userA.setId(101L);
        userRepository.save(userA);

        userB = new User("userb", "userB@example.com", "passwordHash", "USER");
        userB.setId(102L);
        userRepository.save(userB);

        adminUser = new User("admin", "admin@example.com", "passwordHash", "ADMIN");
        adminUser.setId(999L);
        userRepository.save(adminUser);

        repoA = new Repository(userA, 1111L, "repo-a", "usera/repo-a", "main", "https://github.com/usera/repo-a", true);
        repoA.setId(1L);
        repositoryRepository.save(repoA);

        repoB = new Repository(userB, 2222L, "repo-b", "userb/repo-b", "main", "https://github.com/userb/repo-b", true);
        repoB.setId(2L);
        repositoryRepository.save(repoB);
    }

    @Test
    @DisplayName("User A can retrieve their own repository by ID")
    void testGetRepositoryById_OwnerUserA_ReturnsRepository() {
        currentUserService.setAuthenticatedUser(userA.getId(), userA.getUsername(), "ROLE_USER");

        RepositoryResponse response = service.getRepositoryById(repoA.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(repoA.getId());
        assertThat(response.getFullName()).isEqualTo("usera/repo-a");
        assertThat(response.getUserId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("User B cannot retrieve User A's repository by ID (returns 404 to avoid existence leakage)")
    void testGetRepositoryById_CrossUserB_ThrowsResourceNotFoundException() {
        currentUserService.setAuthenticatedUser(userB.getId(), userB.getUsername(), "ROLE_USER");

        assertThatThrownBy(() -> service.getRepositoryById(repoA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with ID: 1");
    }

    @Test
    @DisplayName("Admin can retrieve any user's repository by ID")
    void testGetRepositoryById_AdminUser_ReturnsRepository() {
        currentUserService.setAuthenticatedUser(adminUser.getId(), adminUser.getUsername(), "ROLE_ADMIN");

        RepositoryResponse response = service.getRepositoryById(repoA.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(repoA.getId());
    }

    @Test
    @DisplayName("User A can list their own repositories via getRepositoriesByUserId")
    void testGetRepositoriesByUserId_OwnerUserA_ReturnsUserARepositories() {
        currentUserService.setAuthenticatedUser(userA.getId(), userA.getUsername(), "ROLE_USER");

        List<RepositoryResponse> responses = service.getRepositoriesByUserId(userA.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFullName()).isEqualTo("usera/repo-a");
    }

    @Test
    @DisplayName("User B cannot list User A's repositories by altering userId path parameter (throws 404)")
    void testGetRepositoriesByUserId_CrossUserB_ThrowsResourceNotFoundException() {
        currentUserService.setAuthenticatedUser(userB.getId(), userB.getUsername(), "ROLE_USER");

        assertThatThrownBy(() -> service.getRepositoriesByUserId(userA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: " + userA.getId());
    }

    @Test
    @DisplayName("Admin can list repositories for any user by userId")
    void testGetRepositoriesByUserId_AdminUser_ReturnsTargetUserRepositories() {
        currentUserService.setAuthenticatedUser(adminUser.getId(), adminUser.getUsername(), "ROLE_ADMIN");

        List<RepositoryResponse> responses = service.getRepositoriesByUserId(userA.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFullName()).isEqualTo("usera/repo-a");
    }

    @Test
    @DisplayName("User A can list their own repositories via getCurrentUserRepositories")
    void testGetCurrentUserRepositories_AuthenticatedUserA_ReturnsRepositories() {
        currentUserService.setAuthenticatedUser(userA.getId(), userA.getUsername(), "ROLE_USER");

        List<RepositoryResponse> responses = service.getCurrentUserRepositories();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(repoA.getId());
        assertThat(responses.get(0).getUserId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("Unauthenticated caller listing getCurrentUserRepositories throws AccessDeniedException")
    void testGetCurrentUserRepositories_Unauthenticated_ThrowsAccessDeniedException() {
        currentUserService.clearContext();

        assertThatThrownBy(() -> service.getCurrentUserRepositories())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Full authentication is required to list repositories");
    }

    @Test
    @DisplayName("User B cannot create a repository assigned to User A (throws AccessDeniedException)")
    void testCreateRepository_CrossUserCreation_ThrowsAccessDeniedException() {
        currentUserService.setAuthenticatedUser(userB.getId(), userB.getUsername(), "ROLE_USER");

        RepositoryCreateRequest request = new RepositoryCreateRequest(
                userA.getId(), 9999L, "malicious-repo", "usera/malicious-repo", "main", "https://github.com/usera/malicious-repo", true
        );

        assertThatThrownBy(() -> service.createRepository(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot create repository for another user");
    }

    @Test
    @DisplayName("User A can create a repository assigned to User A")
    void testCreateRepository_OwnerUserA_CreatesRepository() {
        currentUserService.setAuthenticatedUser(userA.getId(), userA.getUsername(), "ROLE_USER");

        RepositoryCreateRequest request = new RepositoryCreateRequest(
                userA.getId(), 8888L, "new-repo", "usera/new-repo", "main", "https://github.com/usera/new-repo", true
        );

        RepositoryResponse response = service.createRepository(request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("usera/new-repo");
        assertThat(response.getUserId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("Internal service workflow with null CurrentUserService succeeds without authorization failure")
    void testInternalWorkflow_NullCurrentUserService_Succeeds() {
        RepositoryService internalService = new RepositoryService(repositoryRepository, userRepository, repositoryMapper, null);

        RepositoryResponse response = internalService.getRepositoryById(repoA.getId());
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(repoA.getId());

        List<RepositoryResponse> list = internalService.getRepositoriesByUserId(userA.getId());
        assertThat(list).hasSize(1);
    }

    @Test
    @DisplayName("Unauthenticated caller accessing getRepositoryById throws AccessDeniedException")
    void testGetRepositoryById_Unauthenticated_ThrowsAccessDeniedException() {
        currentUserService.clearContext();

        assertThatThrownBy(() -> service.getRepositoryById(repoA.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Full authentication is required to access repository metadata");
    }

    @Test
    @DisplayName("Unauthenticated caller accessing getRepositoriesByUserId throws AccessDeniedException")
    void testGetRepositoriesByUserId_Unauthenticated_ThrowsAccessDeniedException() {
        currentUserService.clearContext();

        assertThatThrownBy(() -> service.getRepositoriesByUserId(userA.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Full authentication is required to list repositories");
    }

    @Test
    @DisplayName("Unauthenticated caller accessing createRepository throws AccessDeniedException")
    void testCreateRepository_Unauthenticated_ThrowsAccessDeniedException() {
        currentUserService.clearContext();

        RepositoryCreateRequest request = new RepositoryCreateRequest(
                userA.getId(), 8888L, "new-repo", "usera/new-repo", "main", "https://github.com/usera/new-repo", true
        );

        assertThatThrownBy(() -> service.createRepository(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Full authentication is required to create repositories");
    }

    @Test
    @DisplayName("Authenticated caller with null currentUserId cannot create repository")
    void testCreateRepository_NullCurrentUserId_ThrowsAccessDeniedException() {
        // Authenticated principal exists, but userId cannot be resolved (e.g. database discrepancy)
        currentUserService.setAuthenticatedUser(null, "ghost-user", "ROLE_USER");

        RepositoryCreateRequest request = new RepositoryCreateRequest(
                userA.getId(), 8888L, "new-repo", "usera/new-repo", "main", "https://github.com/usera/new-repo", true
        );

        assertThatThrownBy(() -> service.createRepository(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot create repository for another user");
    }

    // --- Stub Implementations ---

    private static class StubCurrentUserService extends CurrentUserService {
        private Long userId;
        private String username;
        private String role;

        public StubCurrentUserService() {
            super(null);
        }

        void setAuthenticatedUser(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        void clearContext() {
            this.userId = null;
            this.username = null;
            this.role = null;
        }

        @Override public boolean isAuthenticated() { return username != null; }
        @Override public String getCurrentUsername() { return username; }
        @Override public Long getCurrentUserId() { return userId; }
        @Override public boolean hasRole(String targetRole) {
            if (role == null) return false;
            String clean = targetRole.toUpperCase().replace("ROLE_", "");
            return clean.equalsIgnoreCase(this.role.replace("ROLE_", ""));
        }
    }

    private static class InMemoryRepositoryRepository implements RepositoryRepository {
        final Map<Long, Repository> database = new HashMap<>();
        private long idGen = 10L;

        @Override
        public <S extends Repository> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            database.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Repository> findById(Long id) {
            return Optional.ofNullable(database.get(id));
        }

        @Override
        public boolean existsById(Long id) {
            return database.containsKey(id);
        }

        @Override
        public List<Repository> findByUserId(Long userId) {
            return database.values().stream()
                    .filter(r -> r.getUser() != null && userId.equals(r.getUser().getId()))
                    .toList();
        }

        @Override
        public boolean existsByGithubRepositoryId(Long githubRepositoryId) {
            return database.values().stream()
                    .anyMatch(r -> githubRepositoryId.equals(r.getGithubRepositoryId()));
        }

        @Override public Optional<Repository> findByIdAndUserId(Long id, Long userId) {
            return database.values().stream()
                    .filter(r -> id.equals(r.getId()) && r.getUser() != null && userId.equals(r.getUser().getId()))
                    .findFirst();
        }

        @Override public Optional<Repository> findByGithubRepositoryId(Long githubRepositoryId) {
            return database.values().stream()
                    .filter(r -> githubRepositoryId.equals(r.getGithubRepositoryId()))
                    .findFirst();
        }

        @Override public Optional<Repository> findByFullNameIgnoreCase(String fullName) {
            return database.values().stream()
                    .filter(r -> fullName != null && fullName.equalsIgnoreCase(r.getFullName()))
                    .findFirst();
        }

        @Override public List<Repository> findByNameIgnoreCase(String name) {
            return database.values().stream()
                    .filter(r -> name != null && name.equalsIgnoreCase(r.getName()))
                    .toList();
        }

        @Override public void flush() {}
        @Override public <S extends Repository> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Repository> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Repository> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Repository getOne(Long aLong) { return database.get(aLong); }
        @Override public Repository getById(Long aLong) { return database.get(aLong); }
        @Override public Repository getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends Repository> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends Repository> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Repository> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<Repository> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(Repository entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Repository> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<Repository> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<Repository> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
    }

    private static class InMemoryUserRepository implements UserRepository {
        final Map<Long, User> database = new HashMap<>();

        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(database.get(id)); }
        @Override public boolean existsById(Long id) { return database.containsKey(id); }
        @Override public Optional<User> findByEmail(String email) { return database.values().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).findFirst(); }
        @Override public Optional<User> findByUsername(String username) { return database.values().stream().filter(u -> username.equalsIgnoreCase(u.getUsername())).findFirst(); }
        @Override public Optional<User> findByUsernameOrEmail(String username, String email) {
            return database.values().stream().filter(u -> (username != null && username.equalsIgnoreCase(u.getUsername())) || (email != null && email.equalsIgnoreCase(u.getEmail()))).findFirst();
        }
        @Override public Optional<User> findByGithubId(Long githubId) { return database.values().stream().filter(u -> githubId.equals(u.getGithubId())).findFirst(); }
        @Override public boolean existsByEmail(String email) { return database.values().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail())); }
        @Override public boolean existsByUsername(String username) { return database.values().stream().anyMatch(u -> username.equalsIgnoreCase(u.getUsername())); }
        @Override public boolean existsByGithubId(Long githubId) { return database.values().stream().anyMatch(u -> githubId.equals(u.getGithubId())); }
        @Override public <S extends User> S save(S entity) { database.put(entity.getId(), entity); return entity; }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return database.get(aLong); }
        @Override public User getById(Long aLong) { return database.get(aLong); }
        @Override public User getReferenceById(Long aLong) { return database.get(aLong); }
        @Override public <S extends User> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(Long aLong) { database.remove(aLong); }
        @Override public void delete(User entity) { database.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends User> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { database.clear(); }
        @Override public List<User> findAll(Sort sort) { return new ArrayList<>(database.values()); }
        @Override public Page<User> findAll(Pageable pageable) { return new PageImpl<>(new ArrayList<>(database.values())); }
    }
}
