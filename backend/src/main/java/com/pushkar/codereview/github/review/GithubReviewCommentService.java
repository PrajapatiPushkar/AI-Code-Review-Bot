package com.pushkar.codereview.github.review;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.github.client.GithubPullRequestReviewCommentClient;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentRequest;
import com.pushkar.codereview.github.client.dto.GithubReviewCommentResponse;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GithubReviewCommentService {

    private static final Logger log = LoggerFactory.getLogger(GithubReviewCommentService.class);

    private final GithubPullRequestReviewCommentClient commentClient;
    private final CodeReviewMetrics codeReviewMetrics;

    public GithubReviewCommentService(GithubPullRequestReviewCommentClient commentClient) {
        this(commentClient, null);
    }

    public GithubReviewCommentService(GithubPullRequestReviewCommentClient commentClient,
                                     @org.springframework.beans.factory.annotation.Autowired(required = false) CodeReviewMetrics codeReviewMetrics) {
        this.commentClient = commentClient;
        this.codeReviewMetrics = codeReviewMetrics;
    }

    public List<GithubReviewCommentResponse> postReviewComments(Long installationId,
                                                                 String owner,
                                                                 String repository,
                                                                 long pullRequestNumber,
                                                                 String commitId,
                                                                 ReviewResult reviewResult) {
        validateInputs(installationId, owner, repository, pullRequestNumber, commitId, reviewResult);

        List<ReviewFinding> findings = reviewResult.getFindings();
        if (findings == null || findings.isEmpty()) {
            log.info("No review findings to post for repository={}/{}, prNumber={}", owner, repository, pullRequestNumber);
            return Collections.emptyList();
        }

        log.info("Posting inline review comments to GitHub for repository={}/{}, prNumber={}, totalFindings={}",
                owner, repository, pullRequestNumber, findings.size());

        List<GithubReviewCommentResponse> createdComments = new ArrayList<>();

        for (ReviewFinding finding : findings) {
            if (isEligibleForInlineComment(finding)) {
                String commentBody = formatCommentBody(finding);
                GithubReviewCommentRequest request = new GithubReviewCommentRequest(
                        commentBody,
                        commitId,
                        finding.getFilename(),
                        finding.getLine()
                );

                try {
                    GithubReviewCommentResponse created = commentClient.createReviewComment(
                            installationId, owner, repository, pullRequestNumber, request
                    );
                    if (created != null) {
                        createdComments.add(created);
                    }
                } catch (Exception e) {
                    if (codeReviewMetrics != null) {
                        codeReviewMetrics.recordGithubFailure();
                    }
                    log.error("Failed to post GitHub review comment for repository={}/{}, prNumber={}, line={}: {}",
                            owner, repository, pullRequestNumber, finding.getLine(), e.getMessage());
                    throw e;
                }
            }
        }

        log.info("Posted {} review comments to GitHub PR #{} for repository={}/{}",
                createdComments.size(), pullRequestNumber, owner, repository);

        return createdComments;
    }

    public boolean isEligibleForInlineComment(ReviewFinding finding) {
        if (finding == null) {
            return false;
        }
        String filename = finding.getFilename();
        Integer line = finding.getLine();
        return filename != null && !filename.isBlank() && line != null && line > 0;
    }

    public String formatCommentBody(ReviewFinding finding) {
        if (finding == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("**[AI Review");
        if (finding.getSeverity() != null) {
            sb.append(" - ").append(finding.getSeverity().name());
        }
        if (finding.getCategory() != null) {
            sb.append(" - ").append(finding.getCategory().name());
        }
        sb.append("]**\n");

        if (finding.getMessage() != null && !finding.getMessage().isBlank()) {
            sb.append(finding.getMessage()).append("\n");
        }

        if (finding.getSuggestion() != null && !finding.getSuggestion().isBlank()) {
            sb.append("\n💡 **Suggestion:**\n").append(finding.getSuggestion()).append("\n");
        }

        return sb.toString().trim();
    }

    private void validateInputs(Long installationId, String owner, String repository, long pullRequestNumber, String commitId, ReviewResult reviewResult) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
        if (pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be a positive number");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new IllegalArgumentException("Commit ID must not be blank");
        }
        if (reviewResult == null) {
            throw new IllegalArgumentException("ReviewResult must not be null");
        }
    }
}
