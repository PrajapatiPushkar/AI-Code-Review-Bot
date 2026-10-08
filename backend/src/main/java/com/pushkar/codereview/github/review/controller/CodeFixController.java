package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.github.review.dto.CodeFixProposalResponse;
import com.pushkar.codereview.github.review.dto.CodeFixProposalStatusRequest;
import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.dto.FileExportContent;
import com.pushkar.codereview.github.review.fix.CodeFixService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/code-reviews", "/api/v1/code-reviews"})
public class CodeFixController {

    private final CodeFixService codeFixService;

    public CodeFixController(CodeFixService codeFixService) {
        this.codeFixService = codeFixService;
    }

    @PostMapping("/findings/{findingId}/fix")
    public ResponseEntity<CodeFixResponse> generateProposedFix(
            @PathVariable Long findingId,
            @Valid @RequestBody(required = false) CodeFixRequest request
    ) {
        CodeFixResponse response = codeFixService.generateFix(findingId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/findings/{findingId}/fixes")
    public ResponseEntity<List<CodeFixProposalResponse>> getFindingProposals(@PathVariable Long findingId) {
        List<CodeFixProposalResponse> proposals = codeFixService.getFindingProposals(findingId);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/fixes/{proposalId}")
    public ResponseEntity<CodeFixProposalResponse> getProposal(@PathVariable Long proposalId) {
        CodeFixProposalResponse proposal = codeFixService.getProposal(proposalId);
        return ResponseEntity.ok(proposal);
    }

    @PatchMapping("/fixes/{proposalId}/status")
    public ResponseEntity<CodeFixProposalResponse> updateProposalStatus(
            @PathVariable Long proposalId,
            @Valid @RequestBody CodeFixProposalStatusRequest request
    ) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Target status is required");
        }
        CodeFixProposalResponse updated = codeFixService.updateProposalStatus(proposalId, request.getStatus());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/fixes/{proposalId}/patch")
    public ResponseEntity<String> downloadFixPatch(@PathVariable Long proposalId) {
        String patch = codeFixService.getProposalPatch(proposalId);
        String filename = "ai-fix-proposal-" + proposalId + ".patch";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/x-diff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(patch);
    }

    @GetMapping("/fixes/{proposalId}/proposed-content")
    public ResponseEntity<String> downloadProposedContent(@PathVariable Long proposalId) {
        FileExportContent export = codeFixService.getProposedContent(proposalId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(export.content());
    }

    @GetMapping("/fixes/{proposalId}/original-content")
    public ResponseEntity<String> downloadOriginalContent(@PathVariable Long proposalId) {
        FileExportContent export = codeFixService.getOriginalContent(proposalId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(export.content());
    }
}
