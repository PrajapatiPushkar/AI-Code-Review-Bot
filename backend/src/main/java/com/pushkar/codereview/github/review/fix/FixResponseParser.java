package com.pushkar.codereview.github.review.fix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FixResponseParser {

    private final ObjectMapper objectMapper;

    public FixResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    public FixResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public CodeFixResponse parseResponse(String rawText, Long findingId, String expectedFilePath, String provider) {
        if (rawText == null || rawText.isBlank()) {
            throw new GeminiAiReviewException("Gemini fix response text is null or empty");
        }

        String jsonText = extractJson(rawText);

        try {
            ParsedFixOutput parsed = objectMapper.readValue(jsonText, ParsedFixOutput.class);
            if (parsed == null) {
                throw new GeminiAiReviewException("Parsed Gemini fix output is null");
            }

            String filePath = parsed.getFilePath() != null && !parsed.getFilePath().isBlank()
                    ? parsed.getFilePath().trim()
                    : expectedFilePath;

            String diff = parsed.getUnifiedDiff();
            if (diff == null || diff.isBlank()) {
                diff = synthesizeDiff(filePath, parsed.getOriginalContent(), parsed.getProposedContent());
            } else {
                diff = normalizeDiffHeaders(diff, filePath);
            }

            String explanation = parsed.getExplanation() != null && !parsed.getExplanation().isBlank()
                    ? parsed.getExplanation().trim()
                    : "Proposed fix generated for review finding.";

            return new CodeFixResponse(
                    findingId,
                    filePath,
                    explanation,
                    diff,
                    parsed.getOriginalContent(),
                    parsed.getProposedContent(),
                    Instant.now(),
                    provider,
                    "PROPOSED",
                    parsed.getLimitations()
            );

        } catch (GeminiAiReviewException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiAiReviewException("Failed to parse Gemini response into structured code fix: " + e.getMessage(), e);
        }
    }

    private String extractJson(String text) {
        String trimmed = text.trim();

        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd != -1) {
                trimmed = trimmed.substring(firstLineEnd + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    private String normalizeDiffHeaders(String diff, String filePath) {
        String trimmed = diff.trim();
        if (!trimmed.contains("---") && !trimmed.contains("+++")) {
            return String.format("--- a/%s\n+++ b/%s\n%s", filePath, filePath, trimmed);
        }
        return trimmed;
    }

    private String synthesizeDiff(String filePath, String original, String proposed) {
        if (original == null && proposed == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- a/%s\n+++ b/%s\n@@ -1 +1 @@\n", filePath, filePath));
        if (original != null && !original.isBlank()) {
            for (String line : original.split("\r?\n")) {
                sb.append("-").append(line).append("\n");
            }
        }
        if (proposed != null && !proposed.isBlank()) {
            for (String line : proposed.split("\r?\n")) {
                sb.append("+").append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedFixOutput {
        private String filePath;
        private String explanation;
        private String originalContent;
        private String proposedContent;
        private String unifiedDiff;
        private String limitations;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getOriginalContent() {
            return originalContent;
        }

        public void setOriginalContent(String originalContent) {
            this.originalContent = originalContent;
        }

        public String getProposedContent() {
            return proposedContent;
        }

        public void setProposedContent(String proposedContent) {
            this.proposedContent = proposedContent;
        }

        public String getUnifiedDiff() {
            return unifiedDiff;
        }

        public void setUnifiedDiff(String unifiedDiff) {
            this.unifiedDiff = unifiedDiff;
        }

        public String getLimitations() {
            return limitations;
        }

        public void setLimitations(String limitations) {
            this.limitations = limitations;
        }
    }
}
