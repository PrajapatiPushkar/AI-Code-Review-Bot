package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.policy.RepositoryPolicyService;
import com.pushkar.codereview.policy.RepositoryReviewPolicy;
import com.pushkar.codereview.qualitygate.dto.QualityGateResponse;
import com.pushkar.codereview.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class QualityGateService {

    private static final Logger log = LoggerFactory.getLogger(QualityGateService.class);

    private final CodeReviewRepository reviewRepository;
    private final CodeReviewFindingRepository findingRepository;
    private final CodeReviewQualityGateRepository qualityGateRepository;
    private final QualityGateEvaluator evaluator;
    private final RepositoryPolicyService policyService;
    private final CurrentUserService currentUserService;

    public QualityGateService(CodeReviewRepository reviewRepository,
                              CodeReviewFindingRepository findingRepository,
                              CodeReviewQualityGateRepository qualityGateRepository,
                              QualityGateEvaluator evaluator,
                              RepositoryPolicyService policyService,
                              CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.qualityGateRepository = qualityGateRepository;
        this.evaluator = evaluator;
        this.policyService = policyService;
        this.currentUserService = currentUserService;
    }

    public QualityGateResponse getOrEvaluateQualityGate(Long reviewId) {
        if (reviewId == null || reviewId <= 0) {
            throw new IllegalArgumentException("Review ID must be positive");
        }

        CodeReview review = findAndAuthorizeReview(reviewId);

        // If review is not completed, return on-the-fly NOT_EVALUATED result
        if (review.getStatus() != CodeReviewStatus.COMPLETED) {
            RepositoryReviewPolicy policy = policyService.resolveEffectivePolicy(review.getOwner(), review.getRepository());
            QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(policy, review.getStatus(), List.of());
            return new QualityGateResponse(
                    null,
                    review.getId(),
                    result.status(),
                    result.enabled(),
                    result.failOnSeverity(),
                    result.failureCount(),
                    result.reason(),
                    result.evaluatedAt()
            );
        }

        // Check if evaluation was already persisted for this completed review
        Optional<CodeReviewQualityGate> persistedOpt = qualityGateRepository.findByCodeReviewId(review.getId());
        if (persistedOpt.isPresent()) {
            return mapToResponse(persistedOpt.get());
        }

        // Otherwise evaluate, persist, and return
        RepositoryReviewPolicy policy = policyService.resolveEffectivePolicy(review.getOwner(), review.getRepository());
        List<CodeReviewFinding> findings = (findingRepository != null)
                ? findingRepository.findByCodeReviewIdOrderByFilePathAscLineNumberAsc(review.getId())
                : List.of();

        QualityGateEvaluator.QualityGateEvaluationResult evalResult = evaluator.evaluate(policy, review.getStatus(), findings);

        CodeReviewQualityGate gate = new CodeReviewQualityGate(
                review,
                evalResult.status(),
                evalResult.enabled(),
                evalResult.failOnSeverity(),
                evalResult.failureCount(),
                evalResult.reason(),
                evalResult.evaluatedAt()
        );

        CodeReviewQualityGate saved = qualityGateRepository.save(gate);
        log.info("Evaluated and saved quality gate for review ID={}: status={}, failures={}",
                review.getId(), saved.getStatus(), saved.getFailureCount());

        return mapToResponse(saved);
    }

    public Optional<CodeReviewQualityGate> evaluateAndSave(Long reviewId, RepositoryReviewPolicy policy) {
        if (reviewId == null) {
            return Optional.empty();
        }

        if (qualityGateRepository.existsByCodeReviewId(reviewId)) {
            return qualityGateRepository.findByCodeReviewId(reviewId);
        }

        Optional<CodeReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty() || reviewOpt.get().getStatus() != CodeReviewStatus.COMPLETED) {
            return Optional.empty();
        }

        CodeReview review = reviewOpt.get();
        RepositoryReviewPolicy effectivePolicy = (policy != null)
                ? policy
                : policyService.resolveEffectivePolicy(review.getOwner(), review.getRepository());

        List<CodeReviewFinding> findings = (findingRepository != null)
                ? findingRepository.findByCodeReviewIdOrderByFilePathAscLineNumberAsc(review.getId())
                : List.of();

        QualityGateEvaluator.QualityGateEvaluationResult evalResult = evaluator.evaluate(effectivePolicy, review.getStatus(), findings);

        CodeReviewQualityGate gate = new CodeReviewQualityGate(
                review,
                evalResult.status(),
                evalResult.enabled(),
                evalResult.failOnSeverity(),
                evalResult.failureCount(),
                evalResult.reason(),
                evalResult.evaluatedAt()
        );

        CodeReviewQualityGate saved = qualityGateRepository.save(gate);
        log.info("Persisted completed review quality gate for review ID={}: status={}, failureCount={}",
                review.getId(), saved.getStatus(), saved.getFailureCount());

        return Optional.of(saved);
    }

    public CodeReview findAndAuthorizeReview(Long reviewId) {
        if (reviewId == null || reviewId <= 0) {
            throw new IllegalArgumentException("Review ID must be positive");
        }

        CodeReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodeReview record not found with id: " + reviewId));

        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (review.getUser() != null && !review.getUser().getId().equals(currentUserId)) {
                    log.warn("Access denied for userId={} requesting quality gate for review ID={} owned by userId={}",
                            currentUserId, reviewId, review.getUser().getId());
                    throw new AccessDeniedException("You do not have permission to access this code review");
                }
            }
        }

        return review;
    }

    private QualityGateResponse mapToResponse(CodeReviewQualityGate gate) {
        return new QualityGateResponse(
                gate.getId(),
                gate.getCodeReview() != null ? gate.getCodeReview().getId() : null,
                gate.getStatus(),
                gate.isGateEnabled(),
                gate.getFailOnSeverity(),
                gate.getFailureCount(),
                gate.getReason(),
                gate.getEvaluatedAt()
        );
    }
}
