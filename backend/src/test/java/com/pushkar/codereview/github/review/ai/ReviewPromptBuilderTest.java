package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewPromptBuilderTest {

    private ReviewPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new ReviewPromptBuilder();
    }

    @Test
    void testBuildPrompt_FullInput() {
        ReviewFileInput file1 = new ReviewFileInput("src/UserService.java", "modified", 10, 2, 12, "@@ -1,2 +1,2 @@\n+public void fix() {}", null);
        ReviewFileInput file2 = new ReviewFileInput("src/Binary.png", "added", 0, 0, 0, null, null);

        ReviewInput input = new ReviewInput(
                100L, "my-repo", "octocat/my-repo", "https://github.com/octocat/my-repo", "main",
                200L, 42L, "Fix memory leak", "PR description body content", "open",
                "https://github.com/octocat/my-repo/pull/42", "octocat", "feature-branch", "main",
                null, null, List.of(file1, file2)
        );

        String prompt = promptBuilder.buildPrompt(input);

        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("You are an expert software engineer performing a code review.");
        assertThat(prompt).contains("octocat/my-repo");
        assertThat(prompt).contains("Default Branch: main");
        assertThat(prompt).contains("Pull Request #: 42");
        assertThat(prompt).contains("Title: Fix memory leak");
        assertThat(prompt).contains("Description: PR description body content");
        assertThat(prompt).contains("Head Branch: feature-branch");
        assertThat(prompt).contains("Base Branch: main");
        assertThat(prompt).contains("src/UserService.java");
        assertThat(prompt).contains("@@ -1,2 +1,2 @@");
        assertThat(prompt).contains("src/Binary.png");
        assertThat(prompt).contains("[No patch available or binary file]");
        assertThat(prompt).contains("Respond ONLY with a valid JSON object");
    }

    @Test
    void testBuildPrompt_NullChangedFiles() {
        ReviewInput input = new ReviewInput(
                100L, "my-repo", "octocat/my-repo", "https://github.com/octocat/my-repo", "main",
                200L, 42L, "Empty PR", "No description", "open",
                "https://github.com/octocat/my-repo/pull/42", "octocat", "feature", "main",
                null, null, Collections.emptyList()
        );

        String prompt = promptBuilder.buildPrompt(input);

        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("No changed files provided.");
    }

    @Test
    void testBuildPrompt_NullInputThrowsException() {
        assertThatThrownBy(() -> promptBuilder.buildPrompt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReviewInput must not be null");
    }
}
