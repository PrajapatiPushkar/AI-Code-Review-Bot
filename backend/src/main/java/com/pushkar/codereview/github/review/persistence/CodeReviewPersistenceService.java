package com.pushkar.codereview.github.review.persistence;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CodeReviewPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewPersistenceService.class);

    private final CodeReviewRepository repository;
    private final CodeReviewFindingRepository findingRepository;

    private final java.util.concurrent.ConcurrentMap<Long, Instant> activeReviewHeartbeats = new java.util.concurrent.ConcurrentHashMap<>();

    public CodeReviewPersistenceService(CodeReviewRepository repository) {
        this(repository, null);
    }

    @Autowired
    public CodeReviewPersistenceService(CodeReviewRepository repository, @Autowired(required = false) CodeReviewFindingRepository findingRepository) {
        this.repository = repository;
        this.findingRepository = findingRepository;
    }

    public void registerActiveReview(Long reviewId) {
        if (reviewId != null) {
            activeReviewHeartbeats.put(reviewId, Instant.now());
        }
    }

    public void recordHeartbeat(Long reviewId) {
        if (reviewId != null) {
            activeReviewHeartbeats.put(reviewId, Instant.now());
        }
    }

    public void unregisterActiveReview(Long reviewId) {
        if (reviewId != null) {
            activeReviewHeartbeats.remove(reviewId);
        }
    }

    public boolean isReviewActive(Long reviewId) {
        if (reviewId == null) {
            return false;
        }
        Instant heartbeat = activeReviewHeartbeats.get(reviewId);
        if (heartbeat == null) {
            return false;
        }
        // Active review is considered alive if heartbeat was updated within the last 15 minutes (900s)
        return heartbeat.isAfter(Instant.now().minusSeconds(900));
    }

    public java.util.Optional<Instant> getReviewHeartbeat(Long reviewId) {
        if (reviewId == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(activeReviewHeartbeats.get(reviewId));
    }

    @Transactional
    public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber) {
        return createInProgressReview(installationId, owner, repositoryName, pullRequestNumber, null, null);
    }

    @Transactional
    public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber, User user) {
        return createInProgressReview(installationId, owner, repositoryName, pullRequestNumber, user, null);
    }

    @Transactional
    public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber, User user, String commitSha) {
        CodeReview review = new CodeReview(installationId, owner, repositoryName, pullRequestNumber, user, commitSha);
        review.setStatus(CodeReviewStatus.IN_PROGRESS);
        review.setCreatedAt(Instant.now());
        review.setCompletedAt(null);
        CodeReview saved = repository.save(review);
        registerActiveReview(saved.getId());
        log.info("Created IN_PROGRESS review record [reviewId={}] for repository={}/{}, prNumber={}",
                saved.getId(), owner, repositoryName, pullRequestNumber);
        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CodeReview> findById(Long reviewId) {
        if (repository == null || reviewId == null) {
            return java.util.Optional.empty();
        }
        return repository.findById(reviewId);
    }

    @Transactional
    public java.util.Optional<CodeReview> findDuplicateReview(Long userId, Long installationId, String owner, String repositoryName, Integer pullRequestNumber, String commitSha) {
        if (repository == null) {
            return java.util.Optional.empty();
        }
        List<CodeReview> duplicates = repository.findDuplicateReviews(
                userId, installationId, owner, repositoryName, pullRequestNumber, commitSha,
                List.of(CodeReviewStatus.IN_PROGRESS, CodeReviewStatus.COMPLETED)
        );
        if (duplicates == null || duplicates.isEmpty()) {
            return java.util.Optional.empty();
        }
        java.util.Optional<CodeReview> inProgress = duplicates.stream()
                .filter(r -> r.getStatus() == CodeReviewStatus.IN_PROGRESS)
                .findFirst();
        if (inProgress.isPresent()) {
            CodeReview cr = inProgress.get();
            // If the review is actively running (heartbeat alive), it is definitively in-progress
            if (isReviewActive(cr.getId())) {
                return inProgress;
            }
            // If not actively running in memory, check if older than the 15-minute lease threshold
            Instant staleThreshold = Instant.now().minusSeconds(900);
            if (cr.getCreatedAt() != null && cr.getCreatedAt().isBefore(staleThreshold)) {
                log.warn("Found abandoned stale IN_PROGRESS reviewId={} (created at {}). Marking FAILED to unblock duplicate check.",
                        cr.getId(), cr.getCreatedAt());
                int updatedRows = repository.transitionToFailedIfInProgress(
                        cr.getId(),
                        "FAILED: Review execution timed out or was interrupted by application shutdown",
                        Instant.now()
                );
                if (updatedRows > 0) {
                    cr.setStatus(CodeReviewStatus.FAILED);
                } else {
                    return repository.findById(cr.getId())
                            .filter(r -> r.getStatus() == CodeReviewStatus.COMPLETED);
                }
            } else {
                return inProgress;
            }
        }
        return duplicates.stream()
                .filter(r -> r.getStatus() == CodeReviewStatus.COMPLETED)
                .findFirst();
    }

    @Transactional
    public int recoverStaleInProgressReviews(Instant threshold) {
        if (repository == null) {
            return 0;
        }
        // Minimum threshold of 15 minutes (900s) to prevent prematurely failing legitimate long-running reviews
        Instant effectiveThreshold = (threshold != null && threshold.isBefore(Instant.now().minusSeconds(900)))
                ? threshold
                : Instant.now().minusSeconds(900);
        List<CodeReview> stale = repository.findStaleInProgressReviews(effectiveThreshold);
        if (stale == null || stale.isEmpty()) {
            return 0;
        }
        int recovered = 0;
        for (CodeReview r : stale) {
            // CRITICAL: An actively running review must NEVER be marked FAILED solely due to creation time
            if (isReviewActive(r.getId())) {
                log.info("ReviewId={} is older than threshold but actively running (heartbeat alive); skipping stale recovery", r.getId());
                continue;
            }
            int updatedRows = repository.transitionToFailedIfInProgress(
                    r.getId(),
                    "FAILED: Review execution timed out or was interrupted by application shutdown",
                    Instant.now()
            );
            if (updatedRows > 0) {
                r.setStatus(CodeReviewStatus.FAILED);
                r.setCompletedAt(Instant.now());
                recovered++;
                log.warn("Recovered abandoned stale IN_PROGRESS reviewId={} created at {}", r.getId(), r.getCreatedAt());
            } else {
                log.info("ReviewId={} was concurrently completed or transitioned; skipped stale fail", r.getId());
            }
        }
        return recovered;
    }

    @Transactional
    public List<CodeReviewFinding> saveFindings(Long reviewId, List<ReviewFinding> findings) {
        if (findingRepository == null || findings == null || findings.isEmpty() || reviewId == null) {
            return List.of();
        }
        CodeReview review = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + reviewId));

        List<CodeReviewFinding> entities = findings.stream().map(f -> new CodeReviewFinding(
                review,
                f.getFilename() != null ? f.getFilename() : "UNKNOWN",
                f.getLine() != null ? f.getLine() : 1,
                f.getLine(),
                f.getSeverity() != null ? f.getSeverity() : ReviewFindingSeverity.INFO,
                f.getCategory(),
                f.getMessage(),
                f.getSuggestion()
        )).toList();

        List<CodeReviewFinding> saved = findingRepository.saveAll(entities);
        log.info("Persisted {} findings for reviewId={}", saved.size(), reviewId);
        return saved;
    }

    @Transactional
    public CodeReview markCompleted(Long reviewId, String reviewSummary, int totalFindings, int postedCommentsCount) {
        unregisterActiveReview(reviewId);
        CodeReview review = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + reviewId));
        review.setStatus(CodeReviewStatus.COMPLETED);
        review.setReviewSummary(reviewSummary);
        review.setTotalFindings(totalFindings);
        review.setPostedCommentsCount(postedCommentsCount);
        review.setCompletedAt(Instant.now());
        CodeReview updated = repository.save(review);
        log.info("Marked reviewId={} as COMPLETED: totalFindings={}, postedComments={}", reviewId, totalFindings, postedCommentsCount);
        return updated;
    }

    @Transactional
    public CodeReview markFailed(Long reviewId, String errorMessage) {
        unregisterActiveReview(reviewId);
        String summary = (errorMessage != null && !errorMessage.isBlank())
                ? "FAILED: " + errorMessage
                : "FAILED";
        int updatedRows = repository.transitionToFailedIfInProgress(reviewId, summary, Instant.now());
        CodeReview review = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + reviewId));
        if (updatedRows == 0 && review.getStatus() == CodeReviewStatus.COMPLETED) {
            log.info("ReviewId={} is already COMPLETED; not overwriting with FAILED", reviewId);
            return review;
        }
        review.setStatus(CodeReviewStatus.FAILED);
        review.setReviewSummary(summary);
        review.setCompletedAt(Instant.now());
        log.warn("Marked reviewId={} as FAILED: {}", reviewId, errorMessage);
        return review;
    }
}
