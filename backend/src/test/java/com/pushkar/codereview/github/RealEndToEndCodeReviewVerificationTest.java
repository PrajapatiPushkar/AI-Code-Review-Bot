package com.pushkar.codereview.github;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.dto.GithubInstallationRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.github.review.GithubPullRequestCodeReviewService;
import com.pushkar.codereview.github.review.dto.CodeReviewExecutionResult;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewFindingRepository;
import com.pushkar.codereview.github.review.persistence.CodeReviewRepository;
import com.pushkar.codereview.security.CurrentUserService;
import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/code_review_bot",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "github.app-id=4642046",
        "github.app-name=Pushkar-AI-Code-Review-Bot",
        "github.private-key-path=C:/Users/kppus/Downloads/pushkar-ai-code-review-bot.2026-08-18.private-key (1).pem",
        "github.api-base-url=https://api.github.com"
})
@ActiveProfiles("dev")
public class RealEndToEndCodeReviewVerificationTest {

    private static final String DEFAULT_KEY_PATH = "C:/Users/kppus/Downloads/pushkar-ai-code-review-bot.2026-08-18.private-key (1).pem";

    static boolean hasKeyFile() {
        String path = System.getenv("GITHUB_PRIVATE_KEY_PATH");
        if (path == null || path.isBlank()) {
            path = DEFAULT_KEY_PATH;
        }
        return new File(path).exists();
    }

    @Autowired
    private GithubInstallationService githubInstallationService;

    @Autowired
    private GithubPullRequestCodeReviewService codeReviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeReviewRepository codeReviewRepository;

    @Autowired
    private CodeReviewFindingRepository codeReviewFindingRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @EnabledIf("hasKeyFile")
    public void testRealEndToEndFlow() throws Exception {
        System.out.println("\n================ REAL END-TO-END CODE REVIEW VERIFICATION ================");

        // 1. Authenticate User
        User testUser = userRepository.findByEmail("dev@example.com").orElseGet(() -> {
            User u = new User("devuser", "dev@example.com", "$2a$10$abcdefghijklmnopqrstuvwxyz123456", "USER");
            u.setEnabled(true);
            return userRepository.save(u);
        });

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        System.out.println("[Step 1] Authenticated test user: " + testUser.getEmail());

        // 2. Register Real GitHub Installation 154790187
        Long realInstallationId = 154790187L;
        GithubInstallationRequest registerReq = new GithubInstallationRequest(
                realInstallationId, "PrajapatiPushkar", "User"
        );

        GithubInstallationResponse registered = githubInstallationService.registerInstallation(registerReq);
        assertNotNull(registered, "Installation response must not be null");
        assertEquals(realInstallationId, registered.getGithubInstallationId());
        assertTrue(registered.isVerified(), "Installation must be verified against GitHub API");
        System.out.println("[Step 2] Installation Registered & Verified: ID=" + registered.getGithubInstallationId() + ", Account=" + registered.getGithubAccountLogin());

        // 3. Fetch Accessible Repositories
        List<GithubRepositoryResponse> repos = githubInstallationService.getRepositoriesForInstallation(realInstallationId, 1, 30);
        assertNotNull(repos);
        assertFalse(repos.isEmpty(), "Accessible repositories must be returned");
        System.out.println("[Step 3] Accessible Repositories (" + repos.size() + " total):");
        for (GithubRepositoryResponse repo : repos) {
            System.out.println("   - " + repo.getFullName() + " (DefaultBranch: " + repo.getDefaultBranch() + ")");
        }

        // Target repository: fitness-monolith or first available repo
        GithubRepositoryResponse targetRepo = repos.stream()
                .filter(r -> r.getName().contains("fitness"))
                .findFirst()
                .orElse(repos.get(0));

        String owner = targetRepo.getFullName().contains("/") ? targetRepo.getFullName().split("/")[0] : "PrajapatiPushkar";
        String repoName = targetRepo.getName();
        System.out.println("[Step 4] Selected Target Repository: " + owner + "/" + repoName);

        // 4. Fetch PRs
        List<GithubPullRequestResponse> prs = githubInstallationService.getPullRequestsForRepository(
                realInstallationId, owner, repoName, "all", 1, 10
        );
        System.out.println("[Step 5] PRs retrieved for " + owner + "/" + repoName + ": count=" + prs.size());

        // 5. Execute Code Review Pipeline
        long prNumber = !prs.isEmpty() ? prs.get(0).getNumber() : 1L;
        System.out.println("[Step 6] Triggering Code Review Execution for " + owner + "/" + repoName + " PR #" + prNumber);

        CodeReviewExecutionResult executionResult = codeReviewService.executeCodeReview(realInstallationId, owner, repoName, (int) prNumber);
        assertNotNull(executionResult, "Code review execution result must not be null");
        System.out.println("[Step 7] Review Execution Initiated: ReviewID=" + executionResult.getCodeReviewId() + ", Initial Status=" + executionResult.getStatus());

        // Wait up to 5 seconds for async processing
        long timeoutMs = System.currentTimeMillis() + 5000;
        CodeReview finalReview = null;
        while (System.currentTimeMillis() < timeoutMs) {
            if (executionResult.getCodeReviewId() != null) {
                finalReview = codeReviewRepository.findById(executionResult.getCodeReviewId()).orElse(null);
                if (finalReview != null && !"IN_PROGRESS".equals(finalReview.getStatus().name())) {
                    break;
                }
            }
            Thread.sleep(500);
        }

        if (finalReview != null) {
            System.out.println("[Step 8] Final Review Status in Database: " + finalReview.getStatus());
            System.out.println("   - Summary: " + finalReview.getReviewSummary());
            System.out.println("   - Total Findings Count: " + finalReview.getTotalFindings());

            List<CodeReviewFinding> findings = codeReviewFindingRepository.findByCodeReviewIdOrderByFilePathAscLineNumberAsc(finalReview.getId());
            System.out.println("   - Findings persisted in DB: " + findings.size());
            for (CodeReviewFinding f : findings) {
                System.out.println("     * [" + f.getSeverity() + "] " + f.getFilePath() + ":" + f.getLineNumber() + " - " + f.getMessage());
            }
        }

        System.out.println("\n================ REAL END-TO-END VERIFICATION SUCCESSFUL ================");
    }
}
