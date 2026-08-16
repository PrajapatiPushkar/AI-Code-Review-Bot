package com.pushkar.codereview.github;

import com.pushkar.codereview.config.JwtProperties;
import com.pushkar.codereview.config.SecurityConfig;
import com.pushkar.codereview.exception.DuplicateResourceException;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.GithubInstallationVerificationException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.dto.GithubInstallationRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.security.CustomUserDetailsService;
import com.pushkar.codereview.security.JwtAuthenticationFilter;
import com.pushkar.codereview.security.JwtService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GithubInstallationController.class)
@Import({
        SecurityConfig.class,
        CustomUserDetailsService.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        JwtProperties.class,
        GlobalExceptionHandler.class,
        GithubInstallationControllerTest.TestConfig.class
})
class GithubInstallationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StubUserRepository userRepository;

    @Autowired
    private StubGithubInstallationService installationService;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        installationService.clear();

        User user = new User("octocat", "user@example.com", "hash", "USER");
        user.setId(1L);
        userRepository.save(user);

        User other = new User("othercat", "other@example.com", "hash", "USER");
        other.setId(2L);
        userRepository.save(other);
    }

    @Test
    void testRegisterInstallation_Success_Returns201WithVerifiedFields() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(post("/api/v1/github/installations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installationId\":123456,\"githubAccountLogin\":\"octocat\",\"githubAccountType\":\"User\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.githubInstallationId").value(123456))
                .andExpect(jsonPath("$.githubAccountLogin").value("octocat"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verifiedAt").exists());
    }

    @Test
    void testRegisterInstallation_VerificationFailure_Returns422() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");
        installationService.setVerificationFailureForId(77777L);

        mockMvc.perform(post("/api/v1/github/installations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installationId\":77777,\"githubAccountLogin\":\"fake-login\",\"githubAccountType\":\"User\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("GitHub Installation Verification Failed"));
    }

    @Test
    void testGetRepositories_Success_Returns200() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(get("/api/v1/github/installations/1/repositories?page=1&perPage=30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1296269))
                .andExpect(jsonPath("$[0].name").value("hello-world"));
    }

    @Test
    void testGetRepositories_Unverified_Returns422() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");
        installationService.setUnverifiedForId(99L);

        mockMvc.perform(get("/api/v1/github/installations/99/repositories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testGetPullRequests_Success_Returns200() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(get("/api/v1/github/installations/1/repositories/octocat/hello-world/pull-requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(1347))
                .andExpect(jsonPath("$[0].title").value("Amazing feature"));
    }

    @Test
    void testGetMyInstallations_Authenticated_Returns200() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(get("/api/v1/github/installations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].githubInstallationId").value(123456))
                .andExpect(jsonPath("$[0].githubAccountLogin").value("octocat"));
    }

    @Test
    void testGetInstallationById_Owner_Returns200() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(get("/api/v1/github/installations/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.githubInstallationId").value(123456));
    }

    @Test
    void testGetInstallationById_Missing_Returns404() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(get("/api/v1/github/installations/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void testGetInstallationById_Forbidden_Returns403() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");
        installationService.setAccessDeniedForId(10L);

        mockMvc.perform(get("/api/v1/github/installations/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testDeleteInstallation_Owner_Returns204() throws Exception {
        String token = jwtService.generateToken("user@example.com", "USER");

        mockMvc.perform(delete("/api/v1/github/installations/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/github/installations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // --- Stub Beans ---

    @TestConfiguration
    static class TestConfig {
        @Bean
        public StubUserRepository userRepository() {
            return new StubUserRepository();
        }

        @Bean
        public StubGithubInstallationService githubInstallationService() {
            return new StubGithubInstallationService();
        }
    }

    private static class StubGithubInstallationService extends GithubInstallationService {
        private Long accessDeniedId;
        private Long conflictInstallationId;
        private Long verificationFailureId;
        private Long unverifiedId;

        public StubGithubInstallationService() {
            super(null, null, null, null);
        }

        public void setAccessDeniedForId(Long id) {
            this.accessDeniedId = id;
        }

        public void setConflictForInstallationId(Long installationId) {
            this.conflictInstallationId = installationId;
        }

        public void setVerificationFailureForId(Long installationId) {
            this.verificationFailureId = installationId;
        }

        public void setUnverifiedForId(Long installationId) {
            this.unverifiedId = installationId;
        }

        public void clear() {
            this.accessDeniedId = null;
            this.conflictInstallationId = null;
            this.verificationFailureId = null;
            this.unverifiedId = null;
        }

        @Override
        public GithubInstallationResponse registerInstallation(GithubInstallationRequest request) {
            if (request.getInstallationId().equals(conflictInstallationId)) {
                throw new DuplicateResourceException("GitHub installation ID " + request.getInstallationId() + " is already registered to another user");
            }
            if (request.getInstallationId().equals(verificationFailureId)) {
                throw new GithubInstallationVerificationException("GitHub account login mismatch");
            }
            return new GithubInstallationResponse(1L, 1L, request.getInstallationId(), request.getGithubAccountLogin(), request.getGithubAccountType(), true, Instant.now(), Instant.now(), Instant.now());
        }

        @Override
        public List<GithubRepositoryResponse> getRepositoriesForInstallation(Long installationId, int page, int perPage) {
            if (installationId.equals(unverifiedId)) {
                throw new GithubInstallationVerificationException("Installation not verified");
            }
            GithubRepositoryResponse r = new GithubRepositoryResponse(1296269L, "hello-world", "octocat/hello-world", false, "url", "main");
            return List.of(r);
        }

        @Override
        public List<GithubPullRequestResponse> getPullRequestsForRepository(Long installationId, String owner, String repository, String state, int page, int perPage) {
            if (installationId.equals(unverifiedId)) {
                throw new GithubInstallationVerificationException("Installation not verified");
            }
            GithubPullRequestResponse pr = new GithubPullRequestResponse(1L, 1347L, "Amazing feature", "body", "open", "url", null, null, null, Instant.now(), Instant.now());
            return List.of(pr);
        }

        @Override
        public List<GithubInstallationResponse> getInstallationsForCurrentUser() {
            return List.of(new GithubInstallationResponse(1L, 1L, 123456L, "octocat", "User", true, Instant.now(), Instant.now(), Instant.now()));
        }

        @Override
        public GithubInstallationResponse getInstallationById(Long id) {
            if (id.equals(999L)) {
                throw new ResourceNotFoundException("GitHub installation not found with ID: " + id);
            }
            if (id.equals(accessDeniedId)) {
                throw new AccessDeniedException("You do not have permission to access this installation");
            }
            return new GithubInstallationResponse(id, 1L, 123456L, "octocat", "User", true, Instant.now(), Instant.now(), Instant.now());
        }

        @Override
        public void deleteInstallation(Long id) {
            if (id.equals(999L)) {
                throw new ResourceNotFoundException("GitHub installation not found with ID: " + id);
            }
            if (id.equals(accessDeniedId)) {
                throw new AccessDeniedException("You do not have permission to delete this installation");
            }
        }
    }

    private static class StubUserRepository extends StubJpaRepository<User, Long> implements UserRepository {
        public void clear() { database.clear(); }

        @Override public boolean existsByGithubId(Long githubId) { return false; }
        @Override public boolean existsByUsername(String username) { return false; }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public Optional<User> findByGithubId(Long githubId) { return Optional.empty(); }
        @Override public Optional<User> findByUsername(String username) { return database.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst(); }
        @Override public Optional<User> findByEmail(String email) { return database.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst(); }
        @Override public Optional<User> findByUsernameOrEmail(String username, String email) {
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
