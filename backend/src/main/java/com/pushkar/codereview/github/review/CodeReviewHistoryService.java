package com.pushkar.codereview.github.review;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewResultResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewStatusResponse;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewSpecification;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public Page<CodeReviewHistoryResponse> getCodeReviews(int page, int size, String sort, String statusStr, String owner, String repositoryName, Integer pullRequestNumber) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not exceed 100");
        }
        if (pullRequestNumber != null && pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be positive");
        }

        CodeReviewStatus statusEnum = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                statusEnum = CodeReviewStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status filter: " + statusStr);
            }
        }

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Long targetUserId = null;
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                targetUserId = currentUserService.getCurrentUserId();
            }
        }

        Specification<CodeReview> spec = CodeReviewSpecification.withFilters(targetUserId, statusEnum, owner, repositoryName, pullRequestNumber);
        Page<CodeReview> reviewsPage = repository.findAll(spec, pageable);

        return reviewsPage.map(this::mapToResponse);
    }

    public CodeReviewHistoryResponse getById(Long id) {
        CodeReview review = findAndAuthorizeReview(id);
        return mapToResponse(review);
    }

    public CodeReviewStatusResponse getStatusById(Long id) {
        CodeReview review = findAndAuthorizeReview(id);
        return new CodeReviewStatusResponse(
                review.getId(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getCompletedAt(),
                review.getTotalFindings(),
                review.getPostedCommentsCount(),
                review.getReviewSummary()
        );
    }

    public CodeReviewResultResponse getResultById(Long id) {
        CodeReview review = findAndAuthorizeReview(id);
        return new CodeReviewResultResponse(
                review.getId(),
                review.getInstallationId(),
                review.getOwner(),
                review.getRepository(),
                review.getPullRequestNumber(),
                review.getStatus(),
                review.getReviewSummary(),
                review.getTotalFindings(),
                review.getPostedCommentsCount(),
                review.getCreatedAt(),
                review.getCompletedAt()
        );
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

    private CodeReview findAndAuthorizeReview(Long id) {
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

        return review;
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
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
