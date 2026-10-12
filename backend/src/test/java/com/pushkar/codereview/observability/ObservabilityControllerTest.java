package com.pushkar.codereview.observability;

import com.pushkar.codereview.config.CorrelationIdFilter;
import com.pushkar.codereview.config.JwtProperties;
import com.pushkar.codereview.config.SecurityConfig;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.observability.dto.ObservabilitySummaryDto;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ObservabilityController.class})
@Import({
        SecurityConfig.class,
        CorrelationIdFilter.class,
        CustomUserDetailsService.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        JwtProperties.class,
        GlobalExceptionHandler.class,
        ObservabilityControllerTest.TestConfig.class
})
class ObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StubUserRepository userRepository;

    @Autowired
    private StubObservabilityService observabilityService;

    private String validUserToken;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        User user = new User("monitoredUser", "ops@example.com", "encodedPass", "USER");
        user.setId(10L);
        userRepository.save(user);

        validUserToken = jwtService.generateToken("ops@example.com");

        ObservabilitySummaryDto sampleDto = new ObservabilitySummaryDto(
                new ObservabilitySummaryDto.ApplicationHealthDto("UP", "UP", "UP", "UP"),
                new ObservabilitySummaryDto.ReviewWorkflowMetricsDto(10.0, 2.0, 8.0, 1.0, 0, 1250.0,
                        new ObservabilitySummaryDto.FindingsBySourceDto(15.0, 5.0, 20.0)),
                new ObservabilitySummaryDto.WebhookReliabilityMetricsDto(25.0, 24.0, 1.0, 20.0, 4.0, 19.0, 1.0, 2.0, 3.0, 1.0, 95.0, 1.0, 2.0),
                new ObservabilitySummaryDto.ExternalServicesMetricsDto(
                        new ObservabilitySummaryDto.DependencyMetricDto(10.0, 0.0, 1.0, "CLOSED", 0.0, 800.0),
                        new ObservabilitySummaryDto.DependencyMetricDto(30.0, 1.0, 0.0, "CLOSED", 0.0, 150.0)
                ),
                new ObservabilitySummaryDto.QualityGateMetricsDto(7.0, 1.0, 0.0),
                new ObservabilitySummaryDto.SystemMetricsDto(3600.0, Instant.now())
        );

        observabilityService.setSummaryDto(sampleDto);
    }

    @Test
    void testGetObservabilityMetrics_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/observability/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetObservabilityMetrics_Authenticated_Returns200AndSummaryDto() throws Exception {
        mockMvc.perform(get("/api/v1/observability/metrics")
                        .header("Authorization", "Bearer " + validUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationHealth.status").value("UP"))
                .andExpect(jsonPath("$.applicationHealth.liveness").value("UP"))
                .andExpect(jsonPath("$.applicationHealth.database").value("UP"))
                .andExpect(jsonPath("$.reviewMetrics.completedTotal").value(8.0))
                .andExpect(jsonPath("$.reviewMetrics.findings.ai").value(15.0))
                .andExpect(jsonPath("$.reviewMetrics.findings.deterministicRules").value(5.0))
                .andExpect(jsonPath("$.webhookMetrics.deliveriesReceivedTotal").value(25.0))
                .andExpect(jsonPath("$.webhookMetrics.signaturesValidTotal").value(24.0))
                .andExpect(jsonPath("$.webhookMetrics.signaturesInvalidTotal").value(1.0))
                .andExpect(jsonPath("$.qualityGateMetrics.passedTotal").value(7.0));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public StubUserRepository userRepository() {
            return new StubUserRepository();
        }

        @Bean
        public StubObservabilityService observabilityService() {
            return new StubObservabilityService();
        }
    }

    static class StubObservabilityService extends ObservabilityService {
        private ObservabilitySummaryDto summaryDto;

        public StubObservabilityService() {
            super(null, null, null, null, null, null);
        }

        public void setSummaryDto(ObservabilitySummaryDto summaryDto) {
            this.summaryDto = summaryDto;
        }

        @Override
        public ObservabilitySummaryDto getObservabilitySummary() {
            return summaryDto;
        }
    }

    static class StubUserRepository extends StubJpaRepository<User, Long> implements UserRepository {
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

    static abstract class StubJpaRepository<T, ID> implements org.springframework.data.jpa.repository.JpaRepository<T, ID> {
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
