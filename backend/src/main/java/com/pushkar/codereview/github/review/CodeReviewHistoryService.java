package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CodeReviewHistoryService {

    private final CodeReviewRepository repository;
    private final CurrentUserService currentUserService;

    public CodeReviewHistoryService(CodeReviewRepository repository) {
        this(repository, null);
    }

    public CodeReviewHistoryService(CodeReviewRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    public CodeReviewHistoryResponse getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Review ID must be positive");
        }

        CodeReview review = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + id));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (review.getUser() != null && !review.getUser().getId().equals(currentUserId)) {
                    throw new AccessDeniedException("You do not have permission to access this code review");
                }
            }
        }

        return mapToResponse(review);
    }

    public List<CodeReviewHistoryResponse> getByRepository(String owner, String repositoryName) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        if (repositoryName == null || repositoryName.isBlank()) {
            throw new IllegalArgumentException("Repository must not be blank");
        }

        List<CodeReview> reviews;
        if (currentUserService != null && currentUserService.isAuthenticated() && !currentUserService.hasRole("ADMIN")) {
            Long currentUserId = currentUserService.getCurrentUserId();
            reviews = (currentUserId != null)
                    ? repository.findByUserIdAndOwnerAndRepositoryOrderByCreatedAtDesc(currentUserId, owner, repositoryName)
                    : List.of();
        } else {
            reviews = repository.findByOwnerAndRepositoryOrderByCreatedAtDesc(owner, repositoryName);
        }

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

        List<CodeReview> reviews;
        if (currentUserService != null && currentUserService.isAuthenticated() && !currentUserService.hasRole("ADMIN")) {
            Long currentUserId = currentUserService.getCurrentUserId();
            reviews = (currentUserId != null)
                    ? repository.findByUserIdAndOwnerAndRepositoryAndPullRequestNumber(currentUserId, owner, repositoryName, pullRequestNumber)
                    : List.of();
        } else {
            reviews = repository.findByOwnerAndRepositoryAndPullRequestNumber(owner, repositoryName, pullRequestNumber);
        }

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
