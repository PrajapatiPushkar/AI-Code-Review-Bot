package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewPromptBuilder {

    public String buildPrompt(ReviewInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ReviewInput must not be null");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("You are an expert software engineer performing a code review.\n\n");
        sb.append("Analyze the following pull request details and changed files to perform a thorough code review.\n");
        sb.append("Your task is to:\n");
        sb.append("- Identify real bugs.\n");
        sb.append("- Identify security issues.\n");
        sb.append("- Identify meaningful performance problems.\n");
        sb.append("- Identify maintainability issues.\n");
        sb.append("- Avoid nitpicking.\n");
        sb.append("- Avoid reporting issues that are not actionable.\n");
        sb.append("- Consider the provided patch and surrounding context.\n\n");

        sb.append("### Pull Request Context\n");
        sb.append("- Repository: ").append(input.getRepositoryFullName() != null ? input.getRepositoryFullName() : (input.getRepositoryName() != null ? input.getRepositoryName() : "Unknown")).append("\n");
        if (input.getDefaultBranch() != null) {
            sb.append("- Default Branch: ").append(input.getDefaultBranch()).append("\n");
        }
        if (input.getPullRequestNumber() != null) {
            sb.append("- Pull Request #: ").append(input.getPullRequestNumber()).append("\n");
        }
        sb.append("- Title: ").append(input.getTitle() != null ? input.getTitle() : "N/A").append("\n");
        sb.append("- Description: ").append(input.getBody() != null ? input.getBody() : "N/A").append("\n");
        sb.append("- Head Branch: ").append(input.getHeadBranch() != null ? input.getHeadBranch() : "N/A").append("\n");
        sb.append("- Base Branch: ").append(input.getBaseBranch() != null ? input.getBaseBranch() : "N/A").append("\n\n");

        sb.append("### Changed Files\n");
        List<ReviewFileInput> files = input.getFiles();
        if (files == null || files.isEmpty()) {
            sb.append("No changed files provided.\n\n");
        } else {
            for (ReviewFileInput file : files) {
                if (file == null) continue;
                sb.append("--- File: ").append(file.getFilename() != null ? file.getFilename() : "Unknown").append(" ---\n");
                sb.append("Status: ").append(file.getStatus() != null ? file.getStatus() : "unknown");
                if (file.getPreviousFilename() != null) {
                    sb.append(" (renamed from: ").append(file.getPreviousFilename()).append(")");
                }
                sb.append(" | Additions: ").append(file.getAdditions());
                sb.append(" | Deletions: ").append(file.getDeletions());
                sb.append(" | Changes: ").append(file.getChanges()).append("\n");

                if (file.getPatch() != null && !file.getPatch().isBlank()) {
                    sb.append("Patch:\n```\n").append(file.getPatch()).append("\n```\n");
                } else {
                    sb.append("Patch: [No patch available or binary file]\n");
                }
                sb.append("\n");
            }
        }

        sb.append("### Output Format Instructions\n");
        sb.append("Respond ONLY with a valid JSON object matching the following structure without any additional conversational text or markdown prose outside the JSON:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"Brief executive summary of the code review\",\n");
        sb.append("  \"findings\": [\n");
        sb.append("    {\n");
        sb.append("      \"filename\": \"relative/path/to/file.java\",\n");
        sb.append("      \"line\": 42,\n");
        sb.append("      \"severity\": \"HIGH\",\n");
        sb.append("      \"category\": \"BUG\",\n");
        sb.append("      \"message\": \"Clear explanation of the issue\",\n");
        sb.append("      \"suggestion\": \"Concrete actionable fix or improvement suggestion\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("Allowed values for severity: INFO, LOW, MEDIUM, HIGH, CRITICAL.\n");
        sb.append("Allowed values for category: BUG, SECURITY, PERFORMANCE, CODE_STYLE, MAINTAINABILITY, OTHER.\n");
        sb.append("If no issues are found, return an empty array for \"findings\".\n");

        return sb.toString();
    }
}
