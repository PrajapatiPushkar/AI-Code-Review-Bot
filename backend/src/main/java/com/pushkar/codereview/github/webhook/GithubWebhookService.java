package com.pushkar.codereview.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GithubApiException;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewPersistenceService;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryDetailResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryItemResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliverySummaryResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GithubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookService.class);

    private static final Set<String> SUPPORTED_PR_ACTIONS = Set.of(
            "opened",
            "synchronize",
            "reopened",
            "ready_for_review"
    );

    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final GithubInstallationRepository installationRepository;
    private final RepositoryRepository repositoryRepository;
    private final GithubPullRequestCodeReviewService codeReviewService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final CodeReviewPersistenceService persistenceService;

    public GithubWebhookService(GithubWebhookDeliveryRepository deliveryRepository,
                                GithubInstallationRepository installationRepository,
                                RepositoryRepository repositoryRepository,
                                GithubPullRequestCodeReviewService codeReviewService) {
        this(deliveryRepository, installationRepository, repositoryRepository, codeReviewService, null, null, null);
    }

    public GithubWebhookService(GithubWebhookDeliveryRepository deliveryRepository,
                                GithubInstallationRepository installationRepository,
                                RepositoryRepository repositoryRepository,
                                GithubPullRequestCodeReviewService codeReviewService,
                                CurrentUserService currentUserService) {
        this(deliveryRepository, installationRepository, repositoryRepository, codeReviewService, currentUserService, null, null);
    }

    public GithubWebhookService(GithubWebhookDeliveryRepository deliveryRepository,
                                GithubInstallationRepository installationRepository,
                                RepositoryRepository repositoryRepository,
                                GithubPullRequestCodeReviewService codeReviewService,
                                CurrentUserService currentUserService,
                                ObjectMapper objectMapper) {
        this(deliveryRepository, installationRepository, repositoryRepository, codeReviewService, currentUserService, objectMapper, null);
    }

    @Autowired
    public GithubWebhookService(GithubWebhookDeliveryRepository deliveryRepository,
                                GithubInstallationRepository installationRepository,
                                RepositoryRepository repositoryRepository,
                                GithubPullRequestCodeReviewService codeReviewService,
                                @Autowired(required = false) CurrentUserService currentUserService,
                                @Autowired(required = false) ObjectMapper objectMapper,
                                @Autowired(required = false) CodeReviewPersistenceService persistenceService) {
        this.deliveryRepository = deliveryRepository;
        this.installationRepository = installationRepository;
        this.repositoryRepository = repositoryRepository;
        this.codeReviewService = codeReviewService;
        this.currentUserService = currentUserService;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.persistenceService = persistenceService;
    }

    public GithubWebhookResponse processWebhook(String deliveryId, String eventType, byte[] payloadBytes) {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new IllegalArgumentException("Missing X-GitHub-Delivery header");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Missing X-GitHub-Event header");
        }
        if (payloadBytes == null || payloadBytes.length == 0) {
            throw new IllegalArgumentException("Missing or empty request body");
        }

        // 1. Idempotency Check
        Optional<GithubWebhookDelivery> existingOpt = deliveryRepository.findByDeliveryId(deliveryId);
        if (existingOpt.isPresent()) {
            GithubWebhookDelivery existing = existingOpt.get();
            log.info("Duplicate webhook delivery detected [deliveryId={}]. Skipping re-processing.", deliveryId);
            return GithubWebhookResponse.duplicate(
                    deliveryId,
                    existing.getCodeReviewId(),
                    "Webhook delivery already processed"
            );
        }

        // 2. Parse Payload safely
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            log.warn("Malformed JSON payload received for deliveryId={}: {}", deliveryId, e.getMessage());
            throw new IllegalArgumentException("Malformed JSON payload");
        }

        String action = rootNode.path("action").asText(null);
        String repoFullName = rootNode.path("repository").path("full_name").asText(null);
        Integer prNumber = rootNode.has("pull_request") ? rootNode.path("pull_request").path("number").asInt(-1) : null;
        if (prNumber != null && prNumber <= 0) {
            prNumber = rootNode.path("number").asInt(-1);
        }
        if (prNumber != null && prNumber <= 0) {
            prNumber = null;
        }

        String commitSha = rootNode.path("pull_request").path("head").path("sha").asText(null);
        Long installationId = rootNode.has("installation") ? rootNode.path("installation").path("id").asLong(-1) : null;
        if (installationId != null && installationId <= 0) {
            installationId = null;
        }

        // 3. Persist initial delivery record
        GithubWebhookDelivery delivery = new GithubWebhookDelivery(
                deliveryId,
                eventType,
                action,
                repoFullName,
                prNumber,
                commitSha,
                installationId,
                null
        );

        try {
            delivery = deliveryRepository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException dive) {
            log.info("Concurrent duplicate webhook delivery race condition detected [deliveryId={}].", deliveryId);
            Optional<GithubWebhookDelivery> raceWinner = deliveryRepository.findByDeliveryId(deliveryId);
            Long existingReviewId = raceWinner.map(GithubWebhookDelivery::getCodeReviewId).orElse(null);
            return GithubWebhookResponse.duplicate(
                    deliveryId,
                    existingReviewId,
                    "Concurrent duplicate webhook delivery already processed"
            );
        }

        // 4. Handle Ping event
        if ("ping".equalsIgnoreCase(eventType)) {
            delivery.markIgnored("Ping acknowledged");
            deliveryRepository.save(delivery);
            log.info("Handled GitHub ping event [deliveryId={}].", deliveryId);
            return GithubWebhookResponse.pong(deliveryId, "Ping acknowledged");
        }

        // 5. Filter non-PR events
        if (!"pull_request".equalsIgnoreCase(eventType)) {
            String msg = "Event '" + eventType + "' is not supported";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.info("Ignored unsupported webhook event [deliveryId={}, event={}].", deliveryId, eventType);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 6. Filter unsupported PR actions
        if (action == null || !SUPPORTED_PR_ACTIONS.contains(action.toLowerCase())) {
            String msg = "Pull request action '" + action + "' is not supported";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.info("Ignored unsupported PR action [deliveryId={}, action={}].", deliveryId, action);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 7. Filter draft and closed PRs
        boolean isDraft = rootNode.path("pull_request").path("draft").asBoolean(false);
        if (isDraft) {
            String msg = "Draft pull requests are ignored";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.info("Ignored draft PR [deliveryId={}, repo={}, prNumber={}].", deliveryId, repoFullName, prNumber);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        String state = rootNode.path("pull_request").path("state").asText("open");
        if (!"open".equalsIgnoreCase(state)) {
            String msg = "Pull request state is '" + state + "', only open PRs are reviewed";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.info("Ignored non-open PR [deliveryId={}, state={}].", deliveryId, state);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 8. Extract payload metadata
        String repoName = rootNode.path("repository").path("name").asText(null);
        String ownerLogin = rootNode.path("repository").path("owner").path("login").asText(null);
        long githubRepoId = rootNode.path("repository").path("id").asLong(-1);

        if (installationId == null || installationId <= 0 || repoName == null || ownerLogin == null || prNumber == null) {
            String msg = "Incomplete pull_request payload metadata";
            delivery.markFailed("PERMANENT", msg, false, null);
            deliveryRepository.save(delivery);
            throw new IllegalArgumentException(msg);
        }

        // 9. Server-Side Ownership & Verification Checks
        Optional<GithubInstallation> installationOpt = installationRepository.findByGithubInstallationId(installationId);
        if (installationOpt.isEmpty() || !installationOpt.get().isVerified()) {
            String msg = "Installation not found or not verified";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for unverified/unknown installationId={}", installationId);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        GithubInstallation installation = installationOpt.get();
        delivery.setUser(installation.getUser());
        delivery.setInstallationId(installation.getGithubInstallationId());

        Optional<Repository> repoOpt = repositoryRepository.findByGithubRepositoryId(githubRepoId);
        if (repoOpt.isEmpty() && repoFullName != null) {
            repoOpt = repositoryRepository.findByFullNameIgnoreCase(repoFullName);
        }

        if (repoOpt.isEmpty()) {
            String msg = "Repository is not registered in the system";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for unregistered repository={}", repoFullName);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        Repository repository = repoOpt.get();

        if (repository.getUser() == null || !repository.getUser().getId().equals(installation.getUser().getId())) {
            String msg = "Repository owner does not match installation owner";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for owner mismatch on repository={}", repoFullName);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        if (Boolean.FALSE.equals(repository.getIsActive())) {
            String msg = "Repository is currently inactive";
            delivery.markIgnored(msg);
            deliveryRepository.save(delivery);
            log.info("Ignored webhook for inactive repository={}", repoFullName);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 10. Dispatch to Asynchronous Review Pipeline
        try {
            CodeReviewExecutionResult result = codeReviewService.executeCodeReview(
                    installation.getGithubInstallationId(),
                    ownerLogin,
                    repoName,
                    prNumber,
                    commitSha
            );

            delivery.markCompleted(result.getCodeReviewId());
            deliveryRepository.save(delivery);

            log.info("Webhook review successfully triggered: deliveryId={}, reviewId={}, created={}",
                    deliveryId, result.getCodeReviewId(), result.isCreated());

            if (result.isCreated()) {
                return GithubWebhookResponse.accepted(
                        deliveryId,
                        result.getCodeReviewId(),
                        "Pull request code review queued successfully"
                );
            } else {
                return GithubWebhookResponse.duplicate(
                        deliveryId,
                        result.getCodeReviewId(),
                        "Review already active or completed for this commit SHA"
                );
            }
        } catch (Exception e) {
            log.error("Failed executing code review for webhook deliveryId={}: {}", deliveryId, e.getMessage(), e);
            boolean isTransient = isTransientFailure(e);
            String category = isTransient ? "TRANSIENT" : "PERMANENT";
            delivery.markFailed(category, e.getMessage(), isTransient, null);
            deliveryRepository.save(delivery);
            throw e;
        }
    }

    // --- Retry Operations ---

    @Transactional
    public WebhookDeliveryItemResponse retryDelivery(String deliveryId) {
        GithubWebhookDelivery delivery = findAndAuthorizeDelivery(deliveryId);

        if (!delivery.isEligibleForRetry()) {
            if (delivery.getStatus() == WebhookDeliveryStatus.COMPLETED) {
                throw new IllegalArgumentException("Completed deliveries cannot be retried");
            }
            if (delivery.getStatus() == WebhookDeliveryStatus.IGNORED) {
                throw new IllegalArgumentException("Ignored events cannot be retried");
            }
            if (delivery.getStatus() == WebhookDeliveryStatus.PROCESSING) {
                throw new IllegalArgumentException("Delivery is currently processing and cannot be retried");
            }
            throw new IllegalArgumentException("Delivery is not eligible for retry");
        }

        Instant now = Instant.now();
        Instant staleThreshold = now.minusSeconds(300);

        int claimed = deliveryRepository.claimForRetry(delivery.getId(), now, staleThreshold);
        if (claimed == 0) {
            throw new IllegalStateException("Delivery could not be claimed for retry; it may be processing concurrently");
        }

        // Re-read updated delivery state
        delivery = deliveryRepository.findById(delivery.getId()).orElseThrow();

        String repoName = delivery.getRepository();
        String owner = "";
        String shortRepo = repoName;
        if (repoName != null && repoName.contains("/")) {
            String[] parts = repoName.split("/", 2);
            owner = parts[0];
            shortRepo = parts[1];
        }

        try {
            CodeReviewExecutionResult result = codeReviewService.executeCodeReview(
                    delivery.getInstallationId(),
                    owner,
                    shortRepo,
                    delivery.getPullRequestNumber() != null ? delivery.getPullRequestNumber().longValue() : 1L,
                    delivery.getCommitSha()
            );

            delivery.markCompleted(result.getCodeReviewId());
            delivery = deliveryRepository.save(delivery);
            log.info("Retry executed successfully for deliveryId={}, reviewId={}", deliveryId, result.getCodeReviewId());
        } catch (Exception ex) {
            log.error("Retry execution failed for deliveryId={}: {}", deliveryId, ex.getMessage(), ex);
            boolean isTransient = isTransientFailure(ex);
            String category = isTransient ? "TRANSIENT" : "PERMANENT";
            delivery.markFailed(category, ex.getMessage(), isTransient, null);
            delivery = deliveryRepository.save(delivery);
            throw ex;
        }

        return mapToItemResponse(delivery);
    }

    // --- Recovery Operations (Stale Processing & Scheduled Retries) ---

    @Transactional
    public int recoverStaleProcessingDeliveries(int staleThresholdSeconds) {
        Instant threshold = Instant.now().minusSeconds(staleThresholdSeconds > 0 ? staleThresholdSeconds : 300);
        List<GithubWebhookDelivery> stale = deliveryRepository.findStaleProcessingDeliveries(threshold);
        int recovered = 0;

        for (GithubWebhookDelivery d : stale) {
            log.warn("Evaluating stale PROCESSING webhook delivery [id={}, deliveryId={}].", d.getId(), d.getDeliveryId());

            if (persistenceService != null) {
                Optional<CodeReview> activeReviewOpt = Optional.empty();
                if (d.getCodeReviewId() != null) {
                    activeReviewOpt = persistenceService.findById(d.getCodeReviewId());
                } else if (d.getRepository() != null && d.getPullRequestNumber() != null) {
                    String[] parts = d.getRepository().split("/", 2);
                    String owner = parts.length > 1 ? parts[0] : "";
                    String repo = parts.length > 1 ? parts[1] : d.getRepository();
                    Long userId = d.getUser() != null ? d.getUser().getId() : null;
                    activeReviewOpt = persistenceService.findDuplicateReview(
                            userId, d.getInstallationId(), owner, repo, d.getPullRequestNumber(), d.getCommitSha()
                    );
                }

                if (activeReviewOpt.isPresent()) {
                    CodeReview review = activeReviewOpt.get();
                    if (review.getStatus() == CodeReviewStatus.IN_PROGRESS) {
                        d.setStartedAt(Instant.now());
                        deliveryRepository.save(d);
                        log.info("Renewed processing lease for deliveryId={} because reviewId={} is still IN_PROGRESS",
                                d.getDeliveryId(), review.getId());
                        recovered++;
                        continue;
                    } else if (review.getStatus() == CodeReviewStatus.COMPLETED) {
                        d.markCompleted(review.getId());
                        deliveryRepository.save(d);
                        log.info("Marked deliveryId={} as COMPLETED because reviewId={} completed successfully",
                                d.getDeliveryId(), review.getId());
                        recovered++;
                        continue;
                    }
                }
            }

            boolean canRetry = d.getAttemptCount() < d.getMaxAttempts();
            d.markFailed("TIMEOUT", "Processing lease expired after application crash or timeout", canRetry, null);
            deliveryRepository.save(d);
            recovered++;
        }

        return recovered;
    }

    @Transactional
    public int processPendingRetries() {
        List<GithubWebhookDelivery> pending = deliveryRepository.findEligibleScheduledRetries(Instant.now());
        int processed = 0;

        for (GithubWebhookDelivery d : pending) {
            try {
                retryDelivery(d.getDeliveryId());
                processed++;
            } catch (Exception e) {
                log.warn("Automatic retry attempt failed for deliveryId={}: {}", d.getDeliveryId(), e.getMessage());
            }
        }

        return processed;
    }

    // --- Operations Dashboard Queries ---

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryItemResponse> getDeliveries(int page,
                                                           int size,
                                                           String statusStr,
                                                           String repository,
                                                           Instant fromDate,
                                                           Instant toDate) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Long targetUserId = resolveTargetUserId();

        WebhookDeliveryStatus statusEnum = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                statusEnum = WebhookDeliveryStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status filter: " + statusStr);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Specification<GithubWebhookDelivery> spec = WebhookDeliverySpecification.withFilters(
                targetUserId, statusEnum, repository, fromDate, toDate
        );

        Page<GithubWebhookDelivery> results = deliveryRepository.findAll(spec, pageable);
        return results.map(this::mapToItemResponse);
    }

    @Transactional(readOnly = true)
    public WebhookDeliverySummaryResponse getSummary() {
        Long targetUserId = resolveTargetUserId();

        if (targetUserId != null) {
            long total = deliveryRepository.countByUserId(targetUserId);
            long completed = deliveryRepository.countByUserIdAndStatus(targetUserId, WebhookDeliveryStatus.COMPLETED);
            long processing = deliveryRepository.countByUserIdAndStatus(targetUserId, WebhookDeliveryStatus.PROCESSING);
            long failed = deliveryRepository.countByUserIdAndStatus(targetUserId, WebhookDeliveryStatus.FAILED);
            long ignored = deliveryRepository.countByUserIdAndStatus(targetUserId, WebhookDeliveryStatus.IGNORED);
            return new WebhookDeliverySummaryResponse(total, completed, processing, failed, ignored);
        } else {
            long total = deliveryRepository.count();
            long completed = deliveryRepository.countByStatus(WebhookDeliveryStatus.COMPLETED);
            long processing = deliveryRepository.countByStatus(WebhookDeliveryStatus.PROCESSING);
            long failed = deliveryRepository.countByStatus(WebhookDeliveryStatus.FAILED);
            long ignored = deliveryRepository.countByStatus(WebhookDeliveryStatus.IGNORED);
            return new WebhookDeliverySummaryResponse(total, completed, processing, failed, ignored);
        }
    }

    @Transactional(readOnly = true)
    public WebhookDeliveryDetailResponse getDeliveryDetail(String deliveryId) {
        GithubWebhookDelivery delivery = findAndAuthorizeDelivery(deliveryId);
        return mapToDetailResponse(delivery);
    }

    // --- Helper & Authorization Methods ---

    private GithubWebhookDelivery findAndAuthorizeDelivery(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new IllegalArgumentException("Delivery ID must not be blank");
        }

        GithubWebhookDelivery delivery = deliveryRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook delivery not found with ID: " + deliveryId));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (delivery.getUser() == null || !delivery.getUser().getId().equals(currentUserId)) {
                    throw new ResourceNotFoundException("Webhook delivery not found with ID: " + deliveryId);
                }
            }
        }

        return delivery;
    }

    private Long resolveTargetUserId() {
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                return currentUserService.getCurrentUserId();
            }
        }
        return null;
    }

    public boolean isTransientFailure(Throwable t) {
        if (t == null) return false;
        if (t instanceof GithubApiException apiEx) {
            int code = apiEx.getStatusCode();
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
        }
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
        return msg.contains("timeout") || msg.contains("connection refused") || msg.contains("reset by peer")
                || msg.contains("deadlock") || msg.contains("rate limit") || msg.contains("503")
                || msg.contains("502") || msg.contains("504") || msg.contains("temporarily unavailable");
    }

    private WebhookDeliveryItemResponse mapToItemResponse(GithubWebhookDelivery d) {
        WebhookDeliveryItemResponse item = new WebhookDeliveryItemResponse();
        item.setId(d.getId());
        item.setDeliveryId(d.getDeliveryId());
        item.setEventType(d.getEventType());
        item.setAction(d.getAction());
        item.setRepository(d.getRepository());
        item.setPullRequestNumber(d.getPullRequestNumber());
        item.setCommitSha(d.getCommitSha());
        item.setStatus(d.getStatus());
        item.setCodeReviewId(d.getCodeReviewId());
        item.setAttemptCount(d.getAttemptCount());
        item.setMaxAttempts(d.getMaxAttempts());
        item.setRetryable(d.isRetryable());
        item.setRetryEligible(d.isEligibleForRetry());
        item.setErrorCategory(d.getErrorCategory());
        item.setErrorMessage(d.getErrorMessage());
        item.setReceivedAt(d.getReceivedAt());
        item.setStartedAt(d.getStartedAt());
        item.setProcessedAt(d.getProcessedAt());
        item.setNextRetryAt(d.getNextRetryAt());
        return item;
    }

    private WebhookDeliveryDetailResponse mapToDetailResponse(GithubWebhookDelivery d) {
        WebhookDeliveryDetailResponse detail = new WebhookDeliveryDetailResponse();
        detail.setId(d.getId());
        detail.setDeliveryId(d.getDeliveryId());
        detail.setEventType(d.getEventType());
        detail.setAction(d.getAction());
        detail.setRepository(d.getRepository());
        detail.setPullRequestNumber(d.getPullRequestNumber());
        detail.setCommitSha(d.getCommitSha());
        detail.setInstallationId(d.getInstallationId());
        detail.setStatus(d.getStatus());
        detail.setCodeReviewId(d.getCodeReviewId());
        detail.setAttemptCount(d.getAttemptCount());
        detail.setMaxAttempts(d.getMaxAttempts());
        detail.setRetryable(d.isRetryable());
        detail.setRetryEligible(d.isEligibleForRetry());
        detail.setErrorCategory(d.getErrorCategory());
        detail.setErrorMessage(d.getErrorMessage());
        detail.setReceivedAt(d.getReceivedAt());
        detail.setStartedAt(d.getStartedAt());
        detail.setProcessedAt(d.getProcessedAt());
        detail.setNextRetryAt(d.getNextRetryAt());

        if (d.getStartedAt() != null && d.getProcessedAt() != null) {
            detail.setDurationMs(Duration.between(d.getStartedAt(), d.getProcessedAt()).toMillis());
        }

        return detail;
    }
}
