package com.pushkar.codereview.github;

import com.pushkar.codereview.github.dto.GithubInstallationCreateRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/github/installations", "/api/v1/github/installations"})
public class GithubInstallationController {

    private final GithubInstallationService githubInstallationService;

    public GithubInstallationController(GithubInstallationService githubInstallationService) {
        this.githubInstallationService = githubInstallationService;
    }

    @PostMapping
    public ResponseEntity<GithubInstallationResponse> createInstallation(@Valid @RequestBody GithubInstallationCreateRequest request) {
        GithubInstallationResponse response = githubInstallationService.createInstallation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GithubInstallationResponse>> getMyInstallations() {
        List<GithubInstallationResponse> responses = githubInstallationService.getInstallationsForCurrentUser();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GithubInstallationResponse> getInstallationById(@PathVariable Long id) {
        GithubInstallationResponse response = githubInstallationService.getInstallationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GithubInstallationResponse>> getInstallationsByUserId(@PathVariable Long userId) {
        List<GithubInstallationResponse> responses = githubInstallationService.getInstallationsByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstallation(@PathVariable Long id) {
        githubInstallationService.deleteInstallation(id);
        return ResponseEntity.noContent().build();
    }
}
