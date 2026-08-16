package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.github.review.CodeReviewHistoryService;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/code-reviews")
public class CodeReviewHistoryController {

    private final CodeReviewHistoryService historyService;

    public CodeReviewHistoryController(CodeReviewHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeReviewHistoryResponse> getById(@PathVariable Long id) {
        CodeReviewHistoryResponse response = historyService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repository/{owner}/{repository}")
    public ResponseEntity<List<CodeReviewHistoryResponse>> getByRepository(@PathVariable String owner,
                                                                             @PathVariable String repository) {
        List<CodeReviewHistoryResponse> responses = historyService.getByRepository(owner, repository);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/repository/{owner}/{repository}/pull-request/{pullRequestNumber}")
    public ResponseEntity<List<CodeReviewHistoryResponse>> getByPullRequest(@PathVariable String owner,
                                                                             @PathVariable String repository,
                                                                             @PathVariable int pullRequestNumber) {
        List<CodeReviewHistoryResponse> responses = historyService.getByPullRequest(owner, repository, pullRequestNumber);
        return ResponseEntity.ok(responses);
    }
}
