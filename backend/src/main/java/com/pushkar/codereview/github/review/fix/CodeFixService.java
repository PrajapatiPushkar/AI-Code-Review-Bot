package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.review.GithubPullRequestReviewService;
import com.pushkar.codereview.github.review.dto.CodeFixProposalResponse;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.dto.FileExportContent;
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

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CodeFixService {

    private static final Logger log = LoggerFactory.getLogger(CodeFixService.class);

    private final CodeReviewFindingRepository findingRepository;
    private final CurrentUserService currentUserService;
    private final GithubPullRequestReviewService pullRequestReviewService;
    private final AiFixEngine aiFixEngine;
    private final CodeFixProposalRepository proposalRepository;
    private final GeminiProperties geminiProperties;

    public CodeFixService(CodeReviewFindingRepository findingRepository,
                          CurrentUserService currentUserService,
                          AiFixEngine aiFixEngine) {
        this(findingRepository, currentUserService, null, aiFixEngine, null, null);
    }

    public CodeFixService(CodeReviewFindingRepository findingRepository,
                          CurrentUserService currentUserService,
                          GithubPullRequestReviewService pullRequestReviewService,
                          AiFixEngine aiFixEngine) {
        this(findingRepository, currentUserService, pullRequestReviewService, aiFixEngine, null, null);
    }

    @Autowired
    public CodeFixService(CodeReviewFindingRepository findingRepository,
                          CurrentUserService currentUserService,
                          @Autowired(required = false) GithubPullRequestReviewService pullRequestReviewService,
                          AiFixEngine aiFixEngine,
                          @Autowired(required = false) CodeFixProposalRepository proposalRepository,
                          @Autowired(required = false) GeminiProperties geminiProperties) {
        this.findingRepository = findingRepository;
        this.currentUserService = currentUserService;
        this.pullRequestReviewService = pullRequestReviewService;
        this.aiFixEngine = aiFixEngine;
        this.proposalRepository = proposalRepository;
        this.geminiProperties = geminiProperties;
    }

    @Transactional
    public CodeFixResponse generateFix(Long findingId, CodeFixRequest request) {
        if (findingId == null || findingId <= 0) {
            throw new IllegalArgumentException("Finding ID must be positive");
        }

        if (request != null && request.getInstructions() != null && request.getInstructions().length() > 500) {
            throw new IllegalArgumentException("Developer instructions must not exceed 500 characters");
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

        CodeFixResponse response = aiFixEngine.generateFix(input);

        if (proposalRepository != null && response != null) {
            String targetPath = response.getFilePath() != null ? response.getFilePath() : finding.getFilePath();
            String provider = response.getProvider() != null ? response.getProvider() : "Gemini";
            String model = geminiProperties != null && geminiProperties.getModel() != null
                    ? geminiProperties.getModel()
                    : "gemini-3.6-flash";

            CodeFixProposal proposal = new CodeFixProposal(
                finding.getId(),
                targetPath,
                response.getExplanation(),
                response.getUnifiedDiff(),
                response.getOriginalContent(),
                response.getProposedContent(),
                provider,
                model,
                instructions,
                CodeFixProposalStatus.PROPOSED
            );

            CodeFixProposal savedProposal = proposalRepository.save(proposal);
            response.setProposalId(savedProposal.getId());
        }

        return response;
    }

    public List<CodeFixProposalResponse> getFindingProposals(Long findingId) {
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

        if (proposalRepository == null) {
            return Collections.emptyList();
        }

        return proposalRepository.findByFindingIdOrderByCreatedAtDesc(findingId).stream()
                .map(CodeFixProposalResponse::fromEntity)
                .toList();
    }

    public CodeFixProposal getAuthorizedProposal(Long proposalId) {
        if (proposalId == null || proposalId <= 0) {
            throw new IllegalArgumentException("Proposal ID must be positive");
        }

        if (proposalRepository == null) {
            throw new ResourceNotFoundException("Fix proposal not found with id: " + proposalId);
        }

        CodeFixProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Fix proposal not found with id: " + proposalId));

        CodeReviewFinding finding = findingRepository.findById(proposal.getFindingId())
                .orElseThrow(() -> new ResourceNotFoundException("Review finding not found with id: " + proposal.getFindingId()));

        CodeReview review = finding.getCodeReview();
        if (review == null) {
            throw new IllegalStateException("Review finding is not associated with a CodeReview entity");
        }

        authorizeReview(review);

        return proposal;
    }

    public CodeFixProposalResponse getProposal(Long proposalId) {
        return CodeFixProposalResponse.fromEntity(getAuthorizedProposal(proposalId));
    }

    public String getProposalPatch(Long proposalId) {
        CodeFixProposal proposal = getAuthorizedProposal(proposalId);
        return proposal.getUnifiedDiff() != null ? proposal.getUnifiedDiff() : "";
    }

    public FileExportContent getProposedContent(Long proposalId) {
        CodeFixProposal proposal = getAuthorizedProposal(proposalId);
        String safeName = deriveSafeFileName(proposal.getFilePath(), "proposed-file-" + proposalId + ".txt");
        String content = proposal.getProposedContent() != null ? proposal.getProposedContent() : "";
        return new FileExportContent(safeName, content);
    }

    public FileExportContent getOriginalContent(Long proposalId) {
        CodeFixProposal proposal = getAuthorizedProposal(proposalId);
        String safeName = deriveSafeFileName(proposal.getFilePath(), "original-file-" + proposalId + ".txt");
        String content = proposal.getOriginalContent() != null ? proposal.getOriginalContent() : "";
        return new FileExportContent(safeName, content);
    }

    @Transactional
    public CodeFixProposalResponse updateProposalStatus(Long proposalId, CodeFixProposalStatus targetStatus) {
        if (proposalId == null || proposalId <= 0) {
            throw new IllegalArgumentException("Proposal ID must be positive");
        }

        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status is required");
        }

        CodeFixProposal proposal = getAuthorizedProposal(proposalId);

        CodeFixProposalStatus currentStatus = proposal.getStatus();
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", currentStatus, targetStatus)
            );
        }

        proposal.setStatus(targetStatus);
        CodeFixProposal updated = proposalRepository.save(proposal);

        return CodeFixProposalResponse.fromEntity(updated);
    }

    public static String deriveSafeFileName(String filePath, String fallback) {
        if (filePath == null || filePath.isBlank()) {
            return fallback;
        }

        // Normalize slashes
        String normalized = filePath.replace('\\', '/').trim();

        // Extract last segment after slash
        int lastSlash = normalized.lastIndexOf('/');
        String candidate = (lastSlash >= 0) ? normalized.substring(lastSlash + 1).trim() : normalized;

        // Strip path traversal attempts and dangerous characters
        candidate = candidate.replace("..", "").replace("/", "").replace("\\", "").trim();

        // Keep only safe characters: alphanumeric, dots, underscores, hyphens
        candidate = candidate.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Clean leading dots to prevent hidden/special files
        while (candidate.startsWith(".")) {
            candidate = candidate.substring(1);
        }

        if (candidate.isBlank() || candidate.equalsIgnoreCase("txt") || candidate.equals("_")) {
            return fallback;
        }

        return candidate;
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
