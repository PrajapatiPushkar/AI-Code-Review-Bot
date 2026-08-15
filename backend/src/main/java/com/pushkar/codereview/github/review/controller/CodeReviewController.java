package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.dto.CodeReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/code-reviews", "/api/v1/code-reviews"})
public class CodeReviewController {

    private final GithubPullRequestCodeReviewService codeReviewService;

    public CodeReviewController(GithubPullRequestCodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/pull-request")
    public ResponseEntity<CodeReviewExecutionResult> reviewPullRequest(@Valid @RequestBody CodeReviewRequest request) {
        CodeReviewExecutionResult result = codeReviewService.executeCodeReview(
                request.getInstallationId(),
                request.getOwner(),
                request.getRepository(),
                request.getPullRequestNumber()
        );
        return ResponseEntity.ok(result);
    }
}
