package com.pushkar.codereview.github.review.fix;

import org.springframework.stereotype.Component;

@Component
public class FixPromptBuilder {

    public String buildPrompt(FixGenerationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("FixGenerationInput must not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert software engineer generating a precise, minimal proposed code fix for an automated code review finding.\n\n");

        sb.append("### Safety & Implementation Rules\n");
        sb.append("1. Fix ONLY the reported issue; do NOT rewrite surrounding logic or reformat unrelated code.\n");
        sb.append("2. Make the smallest reasonable change needed to resolve the finding.\n");
        sb.append("3. Preserve existing behavior and style outside the finding.\n");
        sb.append("4. Do NOT introduce new external libraries or dependencies.\n");
        sb.append("5. Do NOT modify configuration files, environment variables, credentials, or secrets.\n");
        sb.append("6. Return a valid unified diff with standard headers (--- a/path, +++ b/path, @@ ... @@) and proper + and - line markers.\n");
        sb.append("7. Do NOT mix commentary or conversational prose inside the unified diff.\n\n");

        sb.append("### Review Finding Context\n");
        sb.append("- File Path: ").append(input.getFilePath() != null ? input.getFilePath() : "unknown").append("\n");

        if (input.getLineNumber() != null) {
            if (input.getEndLineNumber() != null && input.getEndLineNumber() > input.getLineNumber()) {
                sb.append("- Target Lines: ").append(input.getLineNumber()).append(" to ").append(input.getEndLineNumber()).append("\n");
            } else {
                sb.append("- Target Line: ").append(input.getLineNumber()).append("\n");
            }
        }

        if (input.getSeverity() != null) {
            sb.append("- Severity: ").append(input.getSeverity()).append("\n");
        }
        if (input.getCategory() != null) {
            sb.append("- Category: ").append(input.getCategory()).append("\n");
        }
        if (input.getSource() != null) {
            sb.append("- Finding Source: ").append(input.getSource());
            if (input.getRuleId() != null) {
                sb.append(" (Rule: ").append(input.getRuleId()).append(")");
            }
            sb.append("\n");
        }
        if (input.getMessage() != null && !input.getMessage().isBlank()) {
            sb.append("- Finding Message: ").append(input.getMessage().trim()).append("\n");
        }
        if (input.getSuggestion() != null && !input.getSuggestion().isBlank()) {
            sb.append("- Existing Recommendation: ").append(input.getSuggestion().trim()).append("\n");
        }

        if (input.getCodeContext() != null && !input.getCodeContext().isBlank()) {
            sb.append("\n### Available Code Context / Diff\n");
            sb.append("```\n").append(input.getCodeContext().trim()).append("\n```\n");
        }

        if (input.getDeveloperInstructions() != null && !input.getDeveloperInstructions().isBlank()) {
            sb.append("\n### Developer Instructions\n");
            sb.append(input.getDeveloperInstructions().trim()).append("\n");
        }

        sb.append("\n### Output Format Instructions\n");
        sb.append("Respond ONLY with a valid JSON object matching the following structure without any conversational text or markdown code fences outside the JSON:\n");
        sb.append("{\n");
        sb.append("  \"filePath\": \"").append(input.getFilePath() != null ? input.getFilePath() : "").append("\",\n");
        sb.append("  \"explanation\": \"Clear and concise explanation of what the fix changes and why\",\n");
        sb.append("  \"originalContent\": \"Exact original lines being modified or replaced\",\n");
        sb.append("  \"proposedContent\": \"Proposed replacement lines\",\n");
        sb.append("  \"unifiedDiff\": \"--- a/...\\n+++ b/...\\n@@ ... @@\\n-old\\n+new\",\n");
        sb.append("  \"limitations\": \"Any assumptions, limitations, or potential side-effects of this fix\"\n");
        sb.append("}\n");

        return sb.toString();
    }
}
