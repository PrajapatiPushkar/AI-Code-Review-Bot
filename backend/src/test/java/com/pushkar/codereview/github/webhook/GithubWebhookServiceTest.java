package com.pushkar.codereview.github.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubWebhookServiceTest {

    private InMemoryDeliveryRepository deliveryRepository;
    private InMemoryInstallationRepository installationRepository;
    private InMemoryRepositoryRepository repositoryRepository;
    private StubCodeReviewService codeReviewService;
    private GithubWebhookService webhookService;
    private User testUser;
    private GithubInstallation testInstallation;
    private Repository testRepo;

    @BeforeEach
    void setUp() {
        deliveryRepository = new InMemoryDeliveryRepository();
        installationRepository = new InMemoryInstallationRepository();
        repositoryRepository = new InMemoryRepositoryRepository();
        codeReviewService = new StubCodeReviewService();

        webhookService = new GithubWebhookService(
                deliveryRepository,
                installationRepository,
                repositoryRepository,
                codeReviewService,
                new ObjectMapper()
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("USER");

        testInstallation = new GithubInstallation(testUser, 12345L, "octocat", "User");
        testInstallation.setVerified(true);
        installationRepository.save(testInstallation);

        testRepo = new Repository(testUser, 9999L, "hello-world", "octocat/hello-world", "main", "https://github.com/octocat/hello-world", true);
        repositoryRepository.save(testRepo);
    }

    private byte[] createPrPayload(String action, boolean isDraft, String state) {
        String json = String.format("""
                {
                  "action": "%s",
                  "number": 42,
                  "pull_request": {
                    "number": 42,
                    "state": "%s",
                    "draft": %b,
                    "head": {
                      "sha": "abc123commit"
                    }
                  },
                  "repository": {
                    "id": 9999,
                    "name": "hello-world",
                    "full_name": "octocat/hello-world",
                    "owner": {
                      "login": "octocat"
                    }
                  },
                  "installation": {
                    "id": 12345
                  }
                }
                """, action, state, isDraft);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void testProcessWebhook_SupportedActionOpened_TriggersReviewSuccessfully() {
        byte[] payload = createPrPayload("opened", false, "open");
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(101L, 12345L, "octocat", "hello-world", 42L, "IN_PROGRESS", "", 0, 0, true, "abc123commit"));

        GithubWebhookResponse response = webhookService.processWebhook("deliv-1", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getReviewId()).isEqualTo(101L);
        assertThat(response.getInitiated()).isTrue();
        assertThat(codeReviewService.wasExecuted()).isTrue();

        GithubWebhookDelivery saved = deliveryRepository.findByDeliveryId("deliv-1").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WebhookDeliveryStatus.COMPLETED);
        assertThat(saved.getCodeReviewId()).isEqualTo(101L);
        assertThat(saved.getRepository()).isEqualTo("octocat/hello-world");
        assertThat(saved.getPullRequestNumber()).isEqualTo(42);
        assertThat(saved.getCommitSha()).isEqualTo("abc123commit");
    }

    @Test
    void testProcessWebhook_SupportedActions_SynchronizeAndReopened() {
        byte[] payloadSync = createPrPayload("synchronize", false, "open");
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(102L, 12345L, "octocat", "hello-world", 42L, "IN_PROGRESS", "", 0, 0, true, "sha-sync"));

        GithubWebhookResponse response1 = webhookService.processWebhook("deliv-sync", "pull_request", payloadSync);
        assertThat(response1.getStatus()).isEqualTo("ACCEPTED");

        byte[] payloadReopen = createPrPayload("reopened", false, "open");
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(103L, 12345L, "octocat", "hello-world", 42L, "IN_PROGRESS", "", 0, 0, true, "sha-reopen"));

        GithubWebhookResponse response2 = webhookService.processWebhook("deliv-reopen", "pull_request", payloadReopen);
        assertThat(response2.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void testProcessWebhook_PingEvent_ReturnsPongAndIgnoredStatus() {
        byte[] payload = "{\"zen\":\"Mind over matter\"}".getBytes(StandardCharsets.UTF_8);

        GithubWebhookResponse response = webhookService.processWebhook("deliv-ping", "ping", payload);

        assertThat(response.getStatus()).isEqualTo("PONG");
        assertThat(codeReviewService.wasExecuted()).isFalse();

        GithubWebhookDelivery saved = deliveryRepository.findByDeliveryId("deliv-ping").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WebhookDeliveryStatus.IGNORED);
    }

    @Test
    void testProcessWebhook_UnsupportedEvent_ReturnsIgnored() {
        byte[] payload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);

        GithubWebhookResponse response = webhookService.processWebhook("deliv-push", "push", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Event 'push' is not supported");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_UnsupportedAction_ReturnsIgnored() {
        byte[] payload = createPrPayload("closed", false, "closed");

        GithubWebhookResponse response = webhookService.processWebhook("deliv-closed", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Pull request action 'closed' is not supported");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_DraftPullRequest_Ignored() {
        byte[] payload = createPrPayload("opened", true, "open");

        GithubWebhookResponse response = webhookService.processWebhook("deliv-draft", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Draft pull requests are ignored");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_NonOpenState_Ignored() {
        byte[] payload = createPrPayload("opened", false, "closed");

        GithubWebhookResponse response = webhookService.processWebhook("deliv-not-open", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("only open PRs are reviewed");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_DuplicateDeliveryId_ReturnsDuplicateWithoutReExecution() {
        byte[] payload = createPrPayload("opened", false, "open");
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(200L, 12345L, "octocat", "hello-world", 42L, "IN_PROGRESS", "", 0, 0, true, "abc"));

        // First delivery
        webhookService.processWebhook("deliv-dup", "pull_request", payload);
        assertThat(codeReviewService.getExecutionCount()).isEqualTo(1);

        // Second delivery with same delivery ID
        GithubWebhookResponse duplicateResponse = webhookService.processWebhook("deliv-dup", "pull_request", payload);

        assertThat(duplicateResponse.getStatus()).isEqualTo("DUPLICATE");
        assertThat(duplicateResponse.getReviewId()).isEqualTo(200L);
        assertThat(codeReviewService.getExecutionCount()).isEqualTo(1); // No new execution!
    }

    @Test
    void testProcessWebhook_ConcurrentDuplicateDelivery_HandledGracefully() {
        byte[] payload = createPrPayload("opened", false, "open");

        // Simulate concurrent insert collision via custom flag
        deliveryRepository.setThrowDataIntegrityViolationOnce(true);
        // Pre-populate winning delivery
        GithubWebhookDelivery winner = new GithubWebhookDelivery("deliv-race", "pull_request", "opened", "octocat/hello-world", 42, "abc");
        winner.setCodeReviewId(777L);
        winner.setStatus(WebhookDeliveryStatus.COMPLETED);
        deliveryRepository.save(winner);

        GithubWebhookResponse response = webhookService.processWebhook("deliv-race-2", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("DUPLICATE");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_UnknownInstallation_Ignored() {
        String json = """
                {
                  "action": "opened",
                  "number": 42,
                  "pull_request": { "number": 42, "state": "open", "draft": false, "head": { "sha": "abc" } },
                  "repository": { "id": 9999, "name": "hello-world", "full_name": "octocat/hello-world", "owner": { "login": "octocat" } },
                  "installation": { "id": 999999 }
                }
                """;

        GithubWebhookResponse response = webhookService.processWebhook("deliv-unknown-inst", "pull_request", json.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Installation not found or not verified");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_UnverifiedInstallation_Ignored() {
        testInstallation.setVerified(false);

        byte[] payload = createPrPayload("opened", false, "open");
        GithubWebhookResponse response = webhookService.processWebhook("deliv-unverified", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Installation not found or not verified");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_UnregisteredRepository_Ignored() {
        String json = """
                {
                  "action": "opened",
                  "number": 42,
                  "pull_request": { "number": 42, "state": "open", "draft": false, "head": { "sha": "abc" } },
                  "repository": { "id": 8888, "name": "secret-repo", "full_name": "other/secret-repo", "owner": { "login": "other" } },
                  "installation": { "id": 12345 }
                }
                """;

        GithubWebhookResponse response = webhookService.processWebhook("deliv-unreg-repo", "pull_request", json.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Repository is not registered in the system");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_InactiveRepository_Ignored() {
        testRepo.setIsActive(false);

        byte[] payload = createPrPayload("opened", false, "open");
        GithubWebhookResponse response = webhookService.processWebhook("deliv-inactive", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        assertThat(response.getMessage()).contains("Repository is currently inactive");
        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_MalformedPayload_ThrowsIllegalArgumentException() {
        byte[] malformed = "not-json{".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> webhookService.processWebhook("deliv-bad-json", "pull_request", malformed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed JSON payload");

        assertThat(codeReviewService.wasExecuted()).isFalse();
    }

    @Test
    void testProcessWebhook_DuplicateShaReview_ReturnsDuplicateStatus() {
        byte[] payload = createPrPayload("opened", false, "open");
        // Simulated existing review returned by CodeReviewService
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(500L, 12345L, "octocat", "hello-world", 42L, "COMPLETED", "Previous review", 2, 1, false, "abc123commit"));

        GithubWebhookResponse response = webhookService.processWebhook("deliv-dup-sha", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("DUPLICATE");
        assertThat(response.getReviewId()).isEqualTo(500L);
        assertThat(response.getInitiated()).isFalse();
    }

    @Test
    void testProcessWebhook_ExecutionFailure_MarksDeliveryFailedAndRethrows() {
        byte[] payload = createPrPayload("opened", false, "open");
        codeReviewService.setExceptionToThrow(new RuntimeException("Simulated AI Review Failure"));

        assertThatThrownBy(() -> webhookService.processWebhook("deliv-fail", "pull_request", payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated AI Review Failure");

        GithubWebhookDelivery saved = deliveryRepository.findByDeliveryId("deliv-fail").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(saved.getErrorMessage()).contains("Simulated AI Review Failure");
    }

    // --- In-Memory Test Helpers ---

    private static class InMemoryDeliveryRepository implements GithubWebhookDeliveryRepository {
        private final List<GithubWebhookDelivery> list = new ArrayList<>();
        private long idGen = 1L;
        private boolean throwDataIntegrityViolationOnce = false;

        public void setThrowDataIntegrityViolationOnce(boolean throwIt) {
            this.throwDataIntegrityViolationOnce = throwIt;
        }

        @Override
        public Optional<GithubWebhookDelivery> findByDeliveryId(String deliveryId) {
            return list.stream().filter(d -> d.getDeliveryId().equals(deliveryId)).findFirst();
        }

        @Override
        public boolean existsByDeliveryId(String deliveryId) {
            return list.stream().anyMatch(d -> d.getDeliveryId().equals(deliveryId));
        }

        @Override
        public <S extends GithubWebhookDelivery> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
                list.add(entity);
            }
            return entity;
        }

        @Override
        public <S extends GithubWebhookDelivery> S saveAndFlush(S entity) {
            if (throwDataIntegrityViolationOnce) {
                throwDataIntegrityViolationOnce = false;
                throw new DataIntegrityViolationException("Unique constraint violation");
            }
            return save(entity);
        }

        @Override public Optional<GithubWebhookDelivery> findById(Long id) { return list.stream().filter(d -> d.getId().equals(id)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<GithubWebhookDelivery> findAll() { return list; }
        @Override public List<GithubWebhookDelivery> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(GithubWebhookDelivery entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends GithubWebhookDelivery> entities) {}
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends GithubWebhookDelivery> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<GithubWebhookDelivery> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public GithubWebhookDelivery getOne(Long aLong) { return null; }
        @Override public GithubWebhookDelivery getById(Long aLong) { return null; }
        @Override public GithubWebhookDelivery getReferenceById(Long aLong) { return null; }
        @Override public <S extends GithubWebhookDelivery> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends GithubWebhookDelivery> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends GithubWebhookDelivery> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends GithubWebhookDelivery> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends GithubWebhookDelivery> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends GithubWebhookDelivery> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends GithubWebhookDelivery, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends GithubWebhookDelivery> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public List<GithubWebhookDelivery> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<GithubWebhookDelivery> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }

    private static class InMemoryInstallationRepository implements GithubInstallationRepository {
        private final List<GithubInstallation> list = new ArrayList<>();
        private long idGen = 1L;

        @Override
        public Optional<GithubInstallation> findByGithubInstallationId(Long githubInstallationId) {
            return list.stream().filter(i -> i.getGithubInstallationId().equals(githubInstallationId)).findFirst();
        }

        @Override public boolean existsByGithubInstallationId(Long githubInstallationId) { return false; }
        @Override public List<GithubInstallation> findByUserId(Long userId) { return List.of(); }
        @Override public <S extends GithubInstallation> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.add(entity);
            return entity;
        }
        @Override public <S extends GithubInstallation> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<GithubInstallation> findById(Long id) { return list.stream().filter(i -> i.getId().equals(id)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<GithubInstallation> findAll() { return list; }
        @Override public List<GithubInstallation> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(GithubInstallation entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends GithubInstallation> entities) {}
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends GithubInstallation> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends GithubInstallation> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<GithubInstallation> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public GithubInstallation getOne(Long aLong) { return null; }
        @Override public GithubInstallation getById(Long aLong) { return null; }
        @Override public GithubInstallation getReferenceById(Long aLong) { return null; }
        @Override public <S extends GithubInstallation> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends GithubInstallation> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends GithubInstallation> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends GithubInstallation> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends GithubInstallation> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends GithubInstallation> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends GithubInstallation, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<GithubInstallation> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<GithubInstallation> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }

    private static class InMemoryRepositoryRepository implements RepositoryRepository {
        private final List<Repository> list = new ArrayList<>();
        private long idGen = 1L;

        @Override
        public Optional<Repository> findByGithubRepositoryId(Long githubRepositoryId) {
            return list.stream().filter(r -> r.getGithubRepositoryId().equals(githubRepositoryId)).findFirst();
        }

        @Override
        public Optional<Repository> findByFullNameIgnoreCase(String fullName) {
            return list.stream().filter(r -> r.getFullName().equalsIgnoreCase(fullName)).findFirst();
        }

        @Override
        public List<Repository> findByNameIgnoreCase(String name) {
            return list.stream().filter(r -> r.getName().equalsIgnoreCase(name)).toList();
        }

        @Override public boolean existsByGithubRepositoryId(Long githubRepositoryId) { return false; }
        @Override public List<Repository> findByUserId(Long userId) { return List.of(); }
        @Override public Optional<Repository> findByIdAndUserId(Long id, Long userId) { return Optional.empty(); }
        @Override public <S extends Repository> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.add(entity);
            return entity;
        }
        @Override public <S extends Repository> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<Repository> findById(Long id) { return list.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Repository> findAll() { return list; }
        @Override public List<Repository> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Repository entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Repository> entities) {}
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends Repository> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Repository> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<Repository> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Repository getOne(Long aLong) { return null; }
        @Override public Repository getById(Long aLong) { return null; }
        @Override public Repository getReferenceById(Long aLong) { return null; }
        @Override public <S extends Repository> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Repository> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends Repository> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends Repository> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends Repository> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Repository> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Repository, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Repository> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<Repository> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }

    private static class StubCodeReviewService extends GithubPullRequestCodeReviewService {
        private CodeReviewExecutionResult resultToReturn;
        private RuntimeException exceptionToThrow;
        private int executionCount = 0;

        public StubCodeReviewService() {
            super(null, null, null, null);
        }

        public void setResultToReturn(CodeReviewExecutionResult result) {
            this.resultToReturn = result;
        }

        public void setExceptionToThrow(RuntimeException ex) {
            this.exceptionToThrow = ex;
        }

        public boolean wasExecuted() {
            return executionCount > 0;
        }

        public int getExecutionCount() {
            return executionCount;
        }

        @Override
        public CodeReviewExecutionResult executeCodeReview(Long requestedInstallationId, String owner, String repository, long pullRequestNumber, String commitSha) {
            executionCount++;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            if (resultToReturn != null) {
                return resultToReturn;
            }
            return new CodeReviewExecutionResult(999L, requestedInstallationId, owner, repository, pullRequestNumber, "IN_PROGRESS", "", 0, 0, true, commitSha);
        }
    }
}
