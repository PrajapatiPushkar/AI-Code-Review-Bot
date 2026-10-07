package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.review.GithubPullRequestReviewService;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CodeFixService {

    private static final Logger log = LoggerFactory.getLogger(CodeFixService.class);

    private final CodeReviewFindingRepository findingRepository;
    private final CurrentUserService currentUserService;
    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiFixEngine aiFixEngine;

    public CodeFixService(CodeReviewFindingRepository findingRepository,
                          CurrentUserService currentUserService,
                          AiFixEngine aiFixEngine) {
        this(findingRepository, currentUserService, null, aiFixEngine);
    }

    @Autowired
    public CodeFixService(CodeReviewFindingRepository findingRepository,
                          CurrentUserService currentUserService,
                          @Autowired(required = false) GithubPullRequestReviewService pullRequestReviewService,
                          AiFixEngine aiFixEngine) {
        this.findingRepository = findingRepository;
        this.currentUserService = currentUserService;
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiFixEngine = aiFixEngine;
    }

    public CodeFixResponse generateFix(Long findingId, CodeFixRequest request) {
        if (findingId == null || findingId <= 0) {
            throw new IllegalArgumentException("Finding ID must be positive");
        }

        CodeReviewFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResourceNotFoundException("Review finding not found with id: " + findingId));

        CodeReview review = finding.getCodeReview();
        if (review == null) {
            throw new IllegalStateException("Review finding is not associated with a CodeReview entity");
        }

        authorizeReview(review);

        String codeContext = null;
        if (pullRequestReviewService != null
                && review.getInstallationId() != null
                && review.getOwner() != null
                && review.getRepository() != null
                && review.getPullRequestNumber() != null) {
            try {
                PullRequestReviewContext prContext = pullRequestReviewService.getReviewContext(
                        review.getInstallationId(),
                        review.getOwner(),
                        review.getRepository(),
                        review.getPullRequestNumber()
                );

                if (prContext != null && prContext.getChangedFiles() != null) {
                    for (GithubPullRequestFileResponse file : prContext.getChangedFiles()) {
                        if (file != null && file.getFilename() != null
                                && file.getFilename().equalsIgnoreCase(finding.getFilePath())) {
                            codeContext = file.getPatch();
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve GitHub PR patch context for finding #{}: {}", findingId, ex.getMessage());
            }
        }

        String instructions = request != null ? request.getInstructions() : null;

        FixGenerationInput input = new FixGenerationInput(
                finding.getId(),
                finding.getFilePath(),
                finding.getLineNumber(),
                finding.getEndLineNumber(),
                finding.getSeverity() != null ? finding.getSeverity().name() : "INFO",
                finding.getCategory() != null ? finding.getCategory().name() : null,
                finding.getMessage(),
                finding.getSuggestion(),
                extractSource(finding),
                extractRuleId(finding),
                codeContext,
                instructions
        );

        return aiFixEngine.generateFix(input);
    }

    private void authorizeReview(CodeReview review) {
        if (currentUserService != null && currentUserService.isAuthenticated()) {
            if (!currentUserService.hasRole("ADMIN")) {
                Long currentUserId = currentUserService.getCurrentUserId();
                if (review.getUser() != null && !review.getUser().getId().equals(currentUserId)) {
                    throw new AccessDeniedException("You do not have permission to access findings for this code review");
                }
            }
        }
    }

    private String extractSource(CodeReviewFinding finding) {
        if (finding.getMessage() != null) {
            String msg = finding.getMessage();
            if (msg.contains("System.out/err") || msg.contains("Empty catch block detected") || msg.contains("Unresolved TODO/FIXME marker")) {
                return "RULE";
            }
        }
        return "AI";
    }

    private String extractRuleId(CodeReviewFinding finding) {
        if (finding.getMessage() != null) {
            String msg = finding.getMessage();
            if (msg.contains("System.out/err")) {
                return "RULE-JAVA-SYSTEM-OUT";
            } else if (msg.contains("Empty catch block detected")) {
                return "RULE-JAVA-EMPTY-CATCH";
            } else if (msg.contains("Unresolved TODO/FIXME marker")) {
                return "RULE-TODO-FIXME";
            }
        }
        return null;
    }
}
