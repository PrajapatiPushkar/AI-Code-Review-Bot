package com.pushkar.codereview.github.review.fix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixPromptBuilderTest {

    private FixPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new FixPromptBuilder();
    }

    @Test
    void testBuildPrompt_IncludesAllRequiredContext() {
        FixGenerationInput input = new FixGenerationInput(
                100L,
                "src/main/java/ActivityService.java",
                42,
                45,
                "HIGH",
                "BUG",
                "Potential null pointer access",
                "Add null check before accessing activity",
                "RULE",
                "RULE-JAVA-NULL",
                "@@ -40,5 +40,5 @@\n if (activity.isValid()) {",
                "Prefer minimal changes."
        );

        String prompt = promptBuilder.buildPrompt(input);

        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("src/main/java/ActivityService.java");
        assertThat(prompt).contains("42 to 45");
        assertThat(prompt).contains("HIGH");
        assertThat(prompt).contains("BUG");
        assertThat(prompt).contains("Potential null pointer access");
        assertThat(prompt).contains("Add null check before accessing activity");
        assertThat(prompt).contains("RULE-JAVA-NULL");
        assertThat(prompt).contains("Prefer minimal changes.");
        assertThat(prompt).contains("unifiedDiff");
        assertThat(prompt).contains("Fix ONLY the reported issue");
    }

    @Test
    void testBuildPrompt_NullInput_ThrowsException() {
        assertThatThrownBy(() -> promptBuilder.buildPrompt(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
