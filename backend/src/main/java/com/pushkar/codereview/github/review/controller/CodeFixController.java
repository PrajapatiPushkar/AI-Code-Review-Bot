package com.pushkar.codereview.github.review.controller;

import com.pushkar.codereview.github.review.dto.CodeFixRequest;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import com.pushkar.codereview.github.review.fix.CodeFixService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/code-reviews/findings", "/api/v1/code-reviews/findings"})
public class CodeFixController {

    private final CodeFixService codeFixService;

    public CodeFixController(CodeFixService codeFixService) {
        this.codeFixService = codeFixService;
    }

    @PostMapping("/{findingId}/fix")
    public ResponseEntity<CodeFixResponse> generateProposedFix(
            @PathVariable Long findingId,
            @Valid @RequestBody(required = false) CodeFixRequest request
    ) {
        CodeFixResponse response = codeFixService.generateFix(findingId, request);
        return ResponseEntity.ok(response);
    }
}
