package com.pushkar.codereview.github.review.persistence;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CodeReviewPersistenceService {

    private final CodeReviewRepository repository;

    public CodeReviewPersistenceService(CodeReviewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber) {
        return createInProgressReview(installationId, owner, repositoryName, pullRequestNumber, null);
    }

    @Transactional
    public CodeReview createInProgressReview(Long installationId, String owner, String repositoryName, Integer pullRequestNumber, User user) {
        CodeReview review = new CodeReview(installationId, owner, repositoryName, pullRequestNumber, user);
        review.setStatus(CodeReviewStatus.IN_PROGRESS);
        review.setCreatedAt(Instant.now());
        review.setCompletedAt(null);
        return repository.save(review);
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
        return repository.save(review);
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
        return repository.save(review);
    }
}
