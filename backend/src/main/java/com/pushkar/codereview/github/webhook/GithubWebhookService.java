package com.pushkar.codereview.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.GithubInstallationRepository;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final ObjectMapper objectMapper;

    public GithubWebhookService(GithubWebhookDeliveryRepository deliveryRepository,
                                GithubInstallationRepository installationRepository,
                                RepositoryRepository repositoryRepository,
                                GithubPullRequestCodeReviewService codeReviewService,
                                ObjectMapper objectMapper) {
        this.deliveryRepository = deliveryRepository;
        this.installationRepository = installationRepository;
        this.repositoryRepository = repositoryRepository;
        this.codeReviewService = codeReviewService;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
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

        // 3. Persist initial delivery record
        GithubWebhookDelivery delivery = new GithubWebhookDelivery(
                deliveryId,
                eventType,
                action,
                repoFullName,
                prNumber,
                commitSha
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
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Handled GitHub ping event [deliveryId={}].", deliveryId);
            return GithubWebhookResponse.pong(deliveryId, "Ping acknowledged");
        }

        // 5. Filter non-PR events
        if (!"pull_request".equalsIgnoreCase(eventType)) {
            String msg = "Event '" + eventType + "' is not supported";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Ignored unsupported webhook event [deliveryId={}, event={}].", deliveryId, eventType);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 6. Filter unsupported PR actions
        if (action == null || !SUPPORTED_PR_ACTIONS.contains(action.toLowerCase())) {
            String msg = "Pull request action '" + action + "' is not supported";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Ignored unsupported PR action [deliveryId={}, action={}].", deliveryId, action);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 7. Filter draft and closed PRs
        boolean isDraft = rootNode.path("pull_request").path("draft").asBoolean(false);
        if (isDraft) {
            String msg = "Draft pull requests are ignored";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Ignored draft PR [deliveryId={}, repo={}, prNumber={}].", deliveryId, repoFullName, prNumber);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        String state = rootNode.path("pull_request").path("state").asText("open");
        if (!"open".equalsIgnoreCase(state)) {
            String msg = "Pull request state is '" + state + "', only open PRs are reviewed";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Ignored non-open PR [deliveryId={}, state={}].", deliveryId, state);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        // 8. Extract payload metadata
        long installationId = rootNode.path("installation").path("id").asLong(-1);
        String repoName = rootNode.path("repository").path("name").asText(null);
        String ownerLogin = rootNode.path("repository").path("owner").path("login").asText(null);
        long githubRepoId = rootNode.path("repository").path("id").asLong(-1);

        if (installationId <= 0 || repoName == null || ownerLogin == null || prNumber == null) {
            String msg = "Incomplete pull_request payload metadata";
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            throw new IllegalArgumentException(msg);
        }

        // 9. Server-Side Ownership & Verification Checks
        Optional<GithubInstallation> installationOpt = installationRepository.findByGithubInstallationId(installationId);
        if (installationOpt.isEmpty() || !installationOpt.get().isVerified()) {
            String msg = "Installation not found or not verified";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for unverified/unknown installationId={}", installationId);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        GithubInstallation installation = installationOpt.get();

        Optional<Repository> repoOpt = repositoryRepository.findByGithubRepositoryId(githubRepoId);
        if (repoOpt.isEmpty() && repoFullName != null) {
            repoOpt = repositoryRepository.findByFullNameIgnoreCase(repoFullName);
        }

        if (repoOpt.isEmpty()) {
            String msg = "Repository is not registered in the system";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for unregistered repository={}", repoFullName);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        Repository repository = repoOpt.get();

        if (repository.getUser() == null || !repository.getUser().getId().equals(installation.getUser().getId())) {
            String msg = "Repository owner does not match installation owner";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            log.warn("Ignored webhook for owner mismatch on repository={}", repoFullName);
            return GithubWebhookResponse.ignored(deliveryId, msg);
        }

        if (Boolean.FALSE.equals(repository.getIsActive())) {
            String msg = "Repository is currently inactive";
            delivery.setStatus(WebhookDeliveryStatus.IGNORED);
            delivery.setErrorMessage(msg);
            delivery.setProcessedAt(Instant.now());
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

            delivery.setCodeReviewId(result.getCodeReviewId());
            delivery.setStatus(WebhookDeliveryStatus.COMPLETED);
            delivery.setProcessedAt(Instant.now());
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
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
            delivery.setProcessedAt(Instant.now());
            deliveryRepository.save(delivery);
            throw e;
        }
    }
}
