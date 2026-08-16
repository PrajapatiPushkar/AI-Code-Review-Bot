package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.github.review.CodeReviewHistoryService;
import com.pushkar.codereview.github.review.dto.CodeReviewFindingResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewHistoryResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewResultResponse;
import com.pushkar.codereview.github.review.dto.CodeReviewStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/code-reviews", "/api/v1/code-reviews"})
public class CodeReviewHistoryController {

    private final CodeReviewHistoryService historyService;

    public CodeReviewHistoryController(CodeReviewHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<Page<CodeReviewHistoryResponse>> getCodeReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) Integer pullRequestNumber
    ) {
        Page<CodeReviewHistoryResponse> responses = historyService.getCodeReviews(page, size, sort, status, owner, repository, pullRequestNumber);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeReviewHistoryResponse> getById(@PathVariable Long id) {
        CodeReviewHistoryResponse response = historyService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<CodeReviewStatusResponse> getStatusById(@PathVariable Long id) {
        CodeReviewStatusResponse response = historyService.getStatusById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<CodeReviewResultResponse> getResultById(@PathVariable Long id) {
        CodeReviewResultResponse response = historyService.getResultById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/findings")
    public ResponseEntity<Page<CodeReviewFindingResponse>> getFindingsByReviewId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lineNumber,asc") String sort
    ) {
        Page<CodeReviewFindingResponse> responses = historyService.getFindingsByReviewId(id, page, size, sort);
        return ResponseEntity.ok(responses);
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
