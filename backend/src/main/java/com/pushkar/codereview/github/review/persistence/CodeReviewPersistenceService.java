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

    public CodeReviewPersistenceService(CodeReviewRepository repository) {
        this(repository, null);
    }

    @Autowired
    public CodeReviewPersistenceService(CodeReviewRepository repository, @Autowired(required = false) CodeReviewFindingRepository findingRepository) {
        this.repository = repository;
        this.findingRepository = findingRepository;
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

    @Transactional(readOnly = true)
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
            return inProgress;
        }
        return duplicates.stream()
                .filter(r -> r.getStatus() == CodeReviewStatus.COMPLETED)
                .findFirst();
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
        CodeReview review = repository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + reviewId));
        review.setStatus(CodeReviewStatus.FAILED);
        if (errorMessage != null && !errorMessage.isBlank()) {
            review.setReviewSummary("FAILED: " + errorMessage);
        }
        review.setCompletedAt(Instant.now());
        CodeReview updated = repository.save(review);
        log.warn("Marked reviewId={} as FAILED: {}", reviewId, errorMessage);
        return updated;
    }
}
