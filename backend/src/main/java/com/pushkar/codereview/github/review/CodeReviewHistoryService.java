package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CodeReviewHistoryService {

    private final CodeReviewRepository repository;

    public CodeReviewHistoryService(CodeReviewRepository repository) {
        this.repository = repository;
    }

    public CodeReviewHistoryResponse getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Review ID must be positive");
        }

        CodeReview review = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + id));

        return mapToResponse(review);
    }

    public List<CodeReviewHistoryResponse> getByRepository(String owner, String repositoryName) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        if (repositoryName == null || repositoryName.isBlank()) {
            throw new IllegalArgumentException("Repository must not be blank");
        }

        List<CodeReview> reviews = repository.findByOwnerAndRepositoryOrderByCreatedAtDesc(owner, repositoryName);

        return reviews.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CodeReviewHistoryResponse> getByPullRequest(String owner, String repositoryName, int pullRequestNumber) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        if (repositoryName == null || repositoryName.isBlank()) {
            throw new IllegalArgumentException("Repository must not be blank");
        }
        if (pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be positive");
        }

        List<CodeReview> reviews = repository.findByOwnerAndRepositoryAndPullRequestNumber(owner, repositoryName, pullRequestNumber);

        return reviews.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CodeReviewHistoryResponse mapToResponse(CodeReview review) {
        if (review == null) {
            return null;
        }
        return new CodeReviewHistoryResponse(
                review.getId(),
                review.getInstallationId(),
                review.getOwner(),
                review.getRepository(),
                review.getPullRequestNumber(),
                review.getReviewSummary(),
                review.getTotalFindings(),
                review.getPostedCommentsCount(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getCompletedAt()
        );
    }
}
