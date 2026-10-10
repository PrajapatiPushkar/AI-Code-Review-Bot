package com.pushkar.codereview.policy;

import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyRequest;
import com.pushkar.codereview.policy.dto.RepositoryReviewPolicyResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/repositories", "/api/v1/repositories"})
public class RepositoryPolicyController {

    private final RepositoryPolicyService policyService;

    public RepositoryPolicyController(RepositoryPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/{repositoryId}/review-policy")
    public ResponseEntity<RepositoryReviewPolicyResponse> getPolicy(@PathVariable Long repositoryId) {
        RepositoryReviewPolicyResponse response = policyService.getEffectivePolicy(repositoryId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{repositoryId}/review-policy")
    public ResponseEntity<RepositoryReviewPolicyResponse> updatePolicy(
            @PathVariable Long repositoryId,
            @Valid @RequestBody RepositoryReviewPolicyRequest request) {
        RepositoryReviewPolicyResponse response = policyService.updatePolicy(repositoryId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{repositoryId}/review-policy/reset")
    public ResponseEntity<RepositoryReviewPolicyResponse> resetPolicy(@PathVariable Long repositoryId) {
        RepositoryReviewPolicyResponse response = policyService.resetPolicy(repositoryId);
        return ResponseEntity.ok(response);
    }
}
