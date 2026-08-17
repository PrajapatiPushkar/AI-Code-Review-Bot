package com.pushkar.codereview.config;

import com.pushkar.codereview.auth.AuthController;
import com.pushkar.codereview.auth.AuthRegistrationService;
import com.pushkar.codereview.auth.AuthService;
import com.pushkar.codereview.auth.dto.LoginRequest;
import com.pushkar.codereview.auth.dto.LoginResponse;
import com.pushkar.codereview.auth.dto.RegisterRequest;
import com.pushkar.codereview.controller.HealthCheckController;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.github.review.CodeReviewHistoryService;
import com.pushkar.codereview.github.review.controller.CodeReviewHistoryController;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.security.CustomUserDetailsService;
import com.pushkar.codereview.security.JwtAuthenticationFilter;
import com.pushkar.codereview.security.JwtService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import com.pushkar.codereview.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HealthCheckController.class,
        CodeReviewHistoryController.class,
        AuthController.class
})
@Import({
        SecurityConfig.class,
        CorrelationIdFilter.class,
        CustomUserDetailsService.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        JwtProperties.class,
        GlobalExceptionHandler.class,
        SecurityConfigTest.TestConfig.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StubUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.clear();

        User testUser = new User("testuser", "test@example.com", passwordEncoder.encode("secretPassword"), "USER");
        testUser.setId(1L);
        userRepository.save(testUser);
    }

    @Test
    void testHealthEndpoint_AccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testRegistrationEndpoint_AccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void testLoginEndpoint_AccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"test@example.com\",\"password\":\"secretPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void testProtectedCodeReviewEndpoint_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/code-reviews/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    @Test
    void testProtectedCodeReviewEndpoint_ValidJwtTokenAccepted() throws Exception {
        String token = jwtService.generateToken("test@example.com", "USER");

        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("octocat"));
    }

    @Test
    void testProtectedCodeReviewEndpoint_DeveloperAndAdminJwtTokenAccepted() throws Exception {
        String devToken = jwtService.generateToken("test@example.com", "DEVELOPER");
        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .header("Authorization", "Bearer " + devToken))
                .andExpect(status().isOk());

        String adminToken = jwtService.generateToken("test@example.com", "ADMIN");
        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedCodeReviewEndpoint_HttpBasicNotAccepted_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/code-reviews/1")
                        .with(httpBasic("test@example.com", "secretPassword")))
                .andExpect(status().isUnauthorized());
    }

    // --- Stub Beans ---

    @TestConfiguration
    static class TestConfig {
        @Bean
        public StubUserRepository userRepository() {
            return new StubUserRepository();
        }

        @Bean
        public CodeReviewHistoryService historyService() {
            return new StubHistoryService();
        }

        @Bean
        public AuthService authService(JwtService jwtService, JwtProperties jwtProperties) {
            return new StubAuthService(jwtService, jwtProperties);
        }
    }

    private static class StubHistoryService extends CodeReviewHistoryService {
        public StubHistoryService() { super(null); }

        @Override
        public CodeReviewHistoryResponse getById(Long id) {
            return new CodeReviewHistoryResponse(
                    id, 12345L, "octocat", "hello-world", 42,
                    "Summary", 1, 1, CodeReviewStatus.COMPLETED, Instant.now(), Instant.now()
            );
        }
    }

    private static class StubAuthService extends AuthService {
        private final JwtService jwtService;

        public StubAuthService(JwtService jwtService, JwtProperties jwtProperties) {
            super(null, jwtService, jwtProperties, null);
            this.jwtService = jwtService;
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            return new UserResponse(10L, null, "newuser", request.getEmail(), null, "USER", Instant.now(), Instant.now());
        }

        @Override
        public LoginResponse login(LoginRequest request) {
            String token = jwtService.generateToken(request.getUsernameOrEmail(), "USER");
            return new LoginResponse(token, "Bearer", 3600000L);
        }
    }

    private static class StubUserRepository extends StubJpaRepository<User, Long> implements UserRepository {
        public void clear() { database.clear(); }

        @Override public boolean existsByGithubId(Long githubId) { return database.values().stream().anyMatch(u -> githubId != null && githubId.equals(u.getGithubId())); }
        @Override public boolean existsByUsername(String username) { return database.values().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username)); }
        @Override public boolean existsByEmail(String email) { return database.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email)); }

        @Override public Optional<User> findByGithubId(Long githubId) { return database.values().stream().filter(u -> githubId != null && githubId.equals(u.getGithubId())).findFirst(); }
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
