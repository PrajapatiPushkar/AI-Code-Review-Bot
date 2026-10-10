package com.pushkar.codereview.github.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryDetailResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryItemResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliverySummaryResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private StubCurrentUserService currentUserService;
    private StubCodeReviewPersistenceService persistenceService;
    private GithubWebhookService webhookService;
    private User testUser;
    private User otherUser;
    private GithubInstallation testInstallation;
    private Repository testRepo;

    @BeforeEach
    void setUp() {
        deliveryRepository = new InMemoryDeliveryRepository();
        installationRepository = new InMemoryInstallationRepository();
        repositoryRepository = new InMemoryRepositoryRepository();
        codeReviewService = new StubCodeReviewService();
        currentUserService = new StubCurrentUserService();
        persistenceService = new StubCodeReviewPersistenceService();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("USER");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setRole("USER");

        currentUserService.setCurrentUser(testUser);

        webhookService = new GithubWebhookService(
                deliveryRepository,
                installationRepository,
                repositoryRepository,
                codeReviewService,
                currentUserService,
                new ObjectMapper(),
                persistenceService
        );

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
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getInstallationId()).isEqualTo(12345L);
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

        webhookService.processWebhook("deliv-dup", "pull_request", payload);
        assertThat(codeReviewService.getExecutionCount()).isEqualTo(1);

        GithubWebhookResponse duplicateResponse = webhookService.processWebhook("deliv-dup", "pull_request", payload);

        assertThat(duplicateResponse.getStatus()).isEqualTo("DUPLICATE");
        assertThat(duplicateResponse.getReviewId()).isEqualTo(200L);
        assertThat(codeReviewService.getExecutionCount()).isEqualTo(1);
    }

    @Test
    void testProcessWebhook_ConcurrentDuplicateDelivery_HandledGracefully() {
        byte[] payload = createPrPayload("opened", false, "open");

        deliveryRepository.setThrowDataIntegrityViolationOnce(true);
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
        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(500L, 12345L, "octocat", "hello-world", 42L, "COMPLETED", "Previous review", 2, 1, false, "abc123commit"));

        GithubWebhookResponse response = webhookService.processWebhook("deliv-dup-sha", "pull_request", payload);

        assertThat(response.getStatus()).isEqualTo("DUPLICATE");
        assertThat(response.getReviewId()).isEqualTo(500L);
        assertThat(response.getInitiated()).isFalse();
    }

    @Test
    void testProcessWebhook_TransientFailure_MarksRetryableAndSetsNextRetry() {
        byte[] payload = createPrPayload("opened", false, "open");
        codeReviewService.setExceptionToThrow(new com.pushkar.codereview.exception.GithubApiException("Temporary 503 from GitHub", 503));

        assertThatThrownBy(() -> webhookService.processWebhook("deliv-transient", "pull_request", payload))
                .isInstanceOf(com.pushkar.codereview.exception.GithubApiException.class);

        GithubWebhookDelivery saved = deliveryRepository.findByDeliveryId("deliv-transient").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(saved.getErrorCategory()).isEqualTo("TRANSIENT");
        assertThat(saved.isRetryable()).isTrue();
        assertThat(saved.getNextRetryAt()).isNotNull();
    }

    @Test
    void testProcessWebhook_PermanentFailure_NotMarkedRetryable() {
        byte[] payload = createPrPayload("opened", false, "open");
        codeReviewService.setExceptionToThrow(new IllegalArgumentException("Invalid PR configuration"));

        assertThatThrownBy(() -> webhookService.processWebhook("deliv-perm", "pull_request", payload))
                .isInstanceOf(IllegalArgumentException.class);

        GithubWebhookDelivery saved = deliveryRepository.findByDeliveryId("deliv-perm").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(saved.getErrorCategory()).isEqualTo("PERMANENT");
        assertThat(saved.isRetryable()).isFalse();
        assertThat(saved.getNextRetryAt()).isNull();
    }

    // --- Retry Operations Tests ---

    @Test
    void testRetryDelivery_EligibleFailedDelivery_SuccessfullyRetried() {
        GithubWebhookDelivery delivery = new GithubWebhookDelivery("deliv-to-retry", "pull_request", "opened", "octocat/hello-world", 42, "sha-retry", 12345L, testUser);
        delivery.markFailed("TRANSIENT", "Temporary network failure", true, 60);
        deliveryRepository.save(delivery);

        codeReviewService.setResultToReturn(new CodeReviewExecutionResult(901L, 12345L, "octocat", "hello-world", 42L, "IN_PROGRESS", "", 0, 0, true, "sha-retry"));

        WebhookDeliveryItemResponse response = webhookService.retryDelivery("deliv-to-retry");

        assertThat(response.getStatus()).isEqualTo(WebhookDeliveryStatus.COMPLETED);
        assertThat(response.getCodeReviewId()).isEqualTo(901L);
        assertThat(response.getAttemptCount()).isEqualTo(2);

        GithubWebhookDelivery updated = deliveryRepository.findByDeliveryId("deliv-to-retry").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookDeliveryStatus.COMPLETED);
        assertThat(updated.getCodeReviewId()).isEqualTo(901L);
    }

    @Test
    void testRetryDelivery_CompletedDelivery_ThrowsIllegalArgumentException() {
        GithubWebhookDelivery delivery = new GithubWebhookDelivery("deliv-completed", "pull_request", "opened", "octocat/hello-world", 42, "sha-c", 12345L, testUser);
        delivery.markCompleted(123L);
        deliveryRepository.save(delivery);

        assertThatThrownBy(() -> webhookService.retryDelivery("deliv-completed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Completed deliveries cannot be retried");
    }

    @Test
    void testRetryDelivery_IgnoredDelivery_ThrowsIllegalArgumentException() {
        GithubWebhookDelivery delivery = new GithubWebhookDelivery("deliv-ignored", "pull_request", "closed", "octocat/hello-world", 42, "sha-i", 12345L, testUser);
        delivery.markIgnored("Unsupported action");
        deliveryRepository.save(delivery);

        assertThatThrownBy(() -> webhookService.retryDelivery("deliv-ignored"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ignored events cannot be retried");
    }

    @Test
    void testRetryDelivery_UnauthorizedUser_ThrowsResourceNotFoundException() {
        GithubWebhookDelivery delivery = new GithubWebhookDelivery("deliv-other-user", "pull_request", "opened", "other/repo", 42, "sha-o", 12345L, otherUser);
        delivery.markFailed("TRANSIENT", "Failed", true, 60);
        deliveryRepository.save(delivery);

        // testUser attempts to retry otherUser's delivery
        assertThatThrownBy(() -> webhookService.retryDelivery("deliv-other-user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Webhook delivery not found");
    }

    @Test
    void testRecoverStaleProcessingDeliveries_RecoversStaleLeases() {
        GithubWebhookDelivery staleDelivery = new GithubWebhookDelivery("deliv-stale", "pull_request", "opened", "octocat/hello-world", 42, "sha-s", 12345L, testUser);
        staleDelivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        staleDelivery.setStartedAt(Instant.now().minusSeconds(600)); // 10 minutes ago
        deliveryRepository.save(staleDelivery);

        int recovered = webhookService.recoverStaleProcessingDeliveries(300);

        assertThat(recovered).isEqualTo(1);
        GithubWebhookDelivery updated = deliveryRepository.findByDeliveryId("deliv-stale").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(updated.getErrorCategory()).isEqualTo("TIMEOUT");
        assertThat(updated.isRetryable()).isTrue();
    }

    @Test
    void testRecoverStaleProcessingDeliveries_WhenReviewStillInProgress_RenewsLeaseAndDoesNotFail() {
        GithubWebhookDelivery staleDelivery = new GithubWebhookDelivery("deliv-active-rev", "pull_request", "opened", "octocat/hello-world", 42, "sha-active", 12345L, testUser);
        staleDelivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        Instant tenMinutesAgo = Instant.now().minusSeconds(600);
        staleDelivery.setStartedAt(tenMinutesAgo);
        staleDelivery.setCodeReviewId(777L);
        deliveryRepository.save(staleDelivery);

        CodeReview inProgressReview = new CodeReview(12345L, "octocat", "hello-world", 42, testUser, "sha-active");
        inProgressReview.setId(777L);
        inProgressReview.setStatus(CodeReviewStatus.IN_PROGRESS);
        persistenceService.addReview(inProgressReview);

        int recovered = webhookService.recoverStaleProcessingDeliveries(300);

        assertThat(recovered).isEqualTo(1);
        GithubWebhookDelivery updated = deliveryRepository.findByDeliveryId("deliv-active-rev").orElseThrow();
        // Concurrency protection: original worker still active -> lease renewed, NOT failed!
        assertThat(updated.getStatus()).isEqualTo(WebhookDeliveryStatus.PROCESSING);
        assertThat(updated.getStartedAt()).isAfter(tenMinutesAgo);
    }

    @Test
    void testRecoverStaleProcessingDeliveries_WhenReviewCompleted_SyncsDeliveryToCompleted() {
        GithubWebhookDelivery staleDelivery = new GithubWebhookDelivery("deliv-done-rev", "pull_request", "opened", "octocat/hello-world", 42, "sha-done", 12345L, testUser);
        staleDelivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        staleDelivery.setStartedAt(Instant.now().minusSeconds(600));
        staleDelivery.setCodeReviewId(888L);
        deliveryRepository.save(staleDelivery);

        CodeReview completedReview = new CodeReview(12345L, "octocat", "hello-world", 42, testUser, "sha-done");
        completedReview.setId(888L);
        completedReview.setStatus(CodeReviewStatus.COMPLETED);
        persistenceService.addReview(completedReview);

        int recovered = webhookService.recoverStaleProcessingDeliveries(300);

        assertThat(recovered).isEqualTo(1);
        GithubWebhookDelivery updated = deliveryRepository.findByDeliveryId("deliv-done-rev").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookDeliveryStatus.COMPLETED);
        assertThat(updated.getCodeReviewId()).isEqualTo(888L);
    }

    @Test
    void testRecoverStaleProcessingDeliveries_CrashedBeforeDispatch_MaxAttemptsBounded() {
        GithubWebhookDelivery staleDelivery = new GithubWebhookDelivery("deliv-crashed-max", "pull_request", "opened", "octocat/hello-world", 42, "sha-max", 12345L, testUser);
        staleDelivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        staleDelivery.setStartedAt(Instant.now().minusSeconds(600));
        staleDelivery.setAttemptCount(3);
        staleDelivery.setMaxAttempts(3);
        deliveryRepository.save(staleDelivery);

        int recovered = webhookService.recoverStaleProcessingDeliveries(300);

        assertThat(recovered).isEqualTo(1);
        GithubWebhookDelivery updated = deliveryRepository.findByDeliveryId("deliv-crashed-max").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(updated.getErrorCategory()).isEqualTo("TIMEOUT");
        assertThat(updated.isRetryable()).isFalse();
        assertThat(updated.getNextRetryAt()).isNull();
    }

    @Test
    void testFindAndAuthorizeDelivery_UnownedDelivery_ThrowsResourceNotFoundForNonAdmin() {
        GithubWebhookDelivery unowned = new GithubWebhookDelivery("deliv-unowned", "pull_request", "opened", "octocat/hello-world", 42, "sha-u", 12345L, null);
        unowned.markFailed("TRANSIENT", "Network err", true, 60);
        deliveryRepository.save(unowned);

        // testUser is non-admin
        assertThatThrownBy(() -> webhookService.getDeliveryDetail("deliv-unowned"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Webhook delivery not found");

        assertThatThrownBy(() -> webhookService.retryDelivery("deliv-unowned"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Webhook delivery not found");
    }

    @Test
    void testClaimForRetry_MaxAttemptsReached_AtomicallyRejectsClaim() {
        GithubWebhookDelivery maxReached = new GithubWebhookDelivery("deliv-max-claim", "pull_request", "opened", "octocat/hello-world", 42, "sha-mc", 12345L, testUser);
        maxReached.markFailed("PERMANENT", "Failed", false, null);
        maxReached.setAttemptCount(3);
        maxReached.setMaxAttempts(3);
        deliveryRepository.save(maxReached);

        Instant now = Instant.now();
        Instant staleThreshold = now.minusSeconds(300);
        int claimed = deliveryRepository.claimForRetry(maxReached.getId(), now, staleThreshold);

        assertThat(claimed).isEqualTo(0);
    }

    @Test
    void testGetSummary_CalculatesAccurateCounts() {
        GithubWebhookDelivery d1 = new GithubWebhookDelivery("d1", "pull_request", "opened", "octocat/hello-world", 1, "sha1", 12345L, testUser);
        d1.markCompleted(1L);
        deliveryRepository.save(d1);

        GithubWebhookDelivery d2 = new GithubWebhookDelivery("d2", "pull_request", "opened", "octocat/hello-world", 2, "sha2", 12345L, testUser);
        d2.markFailed("PERMANENT", "Error", false, null);
        deliveryRepository.save(d2);

        GithubWebhookDelivery d3 = new GithubWebhookDelivery("d3", "pull_request", "opened", "octocat/hello-world", 3, "sha3", 12345L, testUser);
        d3.markIgnored("Draft PR");
        deliveryRepository.save(d3);

        WebhookDeliverySummaryResponse summary = webhookService.getSummary();

        assertThat(summary.getTotalDeliveries()).isEqualTo(3);
        assertThat(summary.getCompletedDeliveries()).isEqualTo(1);
        assertThat(summary.getFailedDeliveries()).isEqualTo(1);
        assertThat(summary.getIgnoredDeliveries()).isEqualTo(1);
        assertThat(summary.getProcessingDeliveries()).isEqualTo(0);
    }

    @Test
    void testGetDeliveryDetail_AuthorizedUser_ReturnsFullDetail() {
        GithubWebhookDelivery d = new GithubWebhookDelivery("d-detail", "pull_request", "opened", "octocat/hello-world", 1, "sha-det", 12345L, testUser);
        d.markCompleted(77L);
        deliveryRepository.save(d);

        WebhookDeliveryDetailResponse detail = webhookService.getDeliveryDetail("d-detail");

        assertThat(detail.getDeliveryId()).isEqualTo("d-detail");
        assertThat(detail.getRepository()).isEqualTo("octocat/hello-world");
        assertThat(detail.getCodeReviewId()).isEqualTo(77L);
        assertThat(detail.getStatus()).isEqualTo(WebhookDeliveryStatus.COMPLETED);
    }

    // --- In-Memory Test Helpers ---

    private static class StubCurrentUserService extends CurrentUserService {
        private User currentUser;
        private boolean authenticated = true;

        public StubCurrentUserService() {
            super(null);
        }

        public void setCurrentUser(User user) {
            this.currentUser = user;
            this.authenticated = (user != null);
        }

        @Override public boolean isAuthenticated() { return authenticated; }
        @Override public User getCurrentUser() { return currentUser; }
        @Override public Long getCurrentUserId() { return currentUser != null ? currentUser.getId() : null; }
        @Override public boolean hasRole(String role) { return currentUser != null && role.equalsIgnoreCase(currentUser.getRole()); }
    }

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
        public Optional<GithubWebhookDelivery> findByDeliveryIdAndUserId(String deliveryId, Long userId) {
            return list.stream()
                    .filter(d -> d.getDeliveryId().equals(deliveryId) && d.getUser() != null && d.getUser().getId().equals(userId))
                    .findFirst();
        }

        @Override
        public boolean existsByDeliveryId(String deliveryId) {
            return list.stream().anyMatch(d -> d.getDeliveryId().equals(deliveryId));
        }

        @Override
        public Page<GithubWebhookDelivery> findByUserId(Long userId, Pageable pageable) {
            List<GithubWebhookDelivery> filtered = list.stream()
                    .filter(d -> d.getUser() != null && d.getUser().getId().equals(userId))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public long countByUserId(Long userId) {
            return list.stream().filter(d -> d.getUser() != null && d.getUser().getId().equals(userId)).count();
        }

        @Override
        public long countByUserIdAndStatus(Long userId, WebhookDeliveryStatus status) {
            return list.stream()
                    .filter(d -> d.getUser() != null && d.getUser().getId().equals(userId) && d.getStatus() == status)
                    .count();
        }

        @Override
        public long countByStatus(WebhookDeliveryStatus status) {
            return list.stream().filter(d -> d.getStatus() == status).count();
        }

        @Override
        public List<GithubWebhookDelivery> findStaleProcessingDeliveries(Instant threshold) {
            return list.stream()
                    .filter(d -> d.getStatus() == WebhookDeliveryStatus.PROCESSING &&
                            ((d.getStartedAt() != null && d.getStartedAt().isBefore(threshold)) ||
                                    (d.getStartedAt() == null && d.getReceivedAt() != null && d.getReceivedAt().isBefore(threshold))))
                    .toList();
        }

        @Override
        public List<GithubWebhookDelivery> findEligibleScheduledRetries(Instant now) {
            return list.stream()
                    .filter(d -> d.getStatus() == WebhookDeliveryStatus.FAILED && d.isRetryable() &&
                            d.getNextRetryAt() != null && !d.getNextRetryAt().isAfter(now) &&
                            d.getAttemptCount() < d.getMaxAttempts())
                    .toList();
        }

        @Override
        public int claimForRetry(Long id, Instant now, Instant staleThreshold) {
            for (GithubWebhookDelivery d : list) {
                if (d.getId().equals(id)) {
                    if (d.getAttemptCount() < d.getMaxAttempts() &&
                            (d.getStatus() == WebhookDeliveryStatus.FAILED ||
                            (d.getStatus() == WebhookDeliveryStatus.PROCESSING &&
                                    (d.getStartedAt() != null && d.getStartedAt().isBefore(staleThreshold))))) {
                        d.markClaimedForRetry();
                        return 1;
                    }
                }
            }
            return 0;
        }

        @Override
        public Page<GithubWebhookDelivery> findAll(Specification<GithubWebhookDelivery> spec, Pageable pageable) {
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
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
        @Override public List<GithubWebhookDelivery> findAll(Specification<GithubWebhookDelivery> spec) { return list; }
        @Override public List<GithubWebhookDelivery> findAll(Specification<GithubWebhookDelivery> spec, org.springframework.data.domain.Sort sort) { return list; }
        @Override public Optional<GithubWebhookDelivery> findOne(Specification<GithubWebhookDelivery> spec) { return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)); }
        @Override public long count(Specification<GithubWebhookDelivery> spec) { return list.size(); }
        @Override public boolean exists(Specification<GithubWebhookDelivery> spec) { return !list.isEmpty(); }
        @Override public long delete(Specification<GithubWebhookDelivery> spec) { return 0; }
        @Override public <S extends GithubWebhookDelivery, R> R findBy(Specification<GithubWebhookDelivery> spec, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
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

    private static class StubCodeReviewPersistenceService extends CodeReviewPersistenceService {
        private final List<CodeReview> reviews = new ArrayList<>();

        public StubCodeReviewPersistenceService() {
            super(null);
        }

        public void addReview(CodeReview review) {
            reviews.add(review);
        }

        @Override
        public Optional<CodeReview> findById(Long reviewId) {
            return reviews.stream().filter(r -> r.getId().equals(reviewId)).findFirst();
        }

        @Override
        public Optional<CodeReview> findDuplicateReview(Long userId, Long installationId, String owner, String repositoryName, Integer pullRequestNumber, String commitSha) {
            return reviews.stream()
                    .filter(r -> r.getRepository().equalsIgnoreCase(repositoryName)
                            && r.getPullRequestNumber().equals(pullRequestNumber)
                            && (commitSha == null || commitSha.equals(r.getCommitSha())))
                    .findFirst();
        }
    }
}
