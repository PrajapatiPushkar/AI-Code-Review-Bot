package com.pushkar.codereview.github.review.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    public GeminiResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    public GeminiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public ReviewResult parseResponse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new GeminiAiReviewException("Gemini response text is null or empty");
        }

        String jsonText = extractJson(rawText);

        try {
            ParsedReviewOutput parsedOutput = objectMapper.readValue(jsonText, ParsedReviewOutput.class);
            if (parsedOutput == null) {
                throw new GeminiAiReviewException("Parsed Gemini review response output is null");
            }

            String summary = parsedOutput.getSummary() != null ? parsedOutput.getSummary() : "No summary provided by AI review.";
            List<ReviewFinding> findings = new ArrayList<>();

            if (parsedOutput.getFindings() != null) {
                for (ParsedFinding rawFinding : parsedOutput.getFindings()) {
                    if (rawFinding == null) continue;
                    findings.add(mapToReviewFinding(rawFinding));
                }
            }

            return new ReviewResult(summary, findings);
        } catch (GeminiAiReviewException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiAiReviewException("Failed to parse Gemini review response into ReviewResult", e);
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

    private ReviewFinding mapToReviewFinding(ParsedFinding raw) {
        ReviewFindingSeverity severity = parseSeverity(raw.getSeverity());
        ReviewFindingCategory category = parseCategory(raw.getCategory());

        return new ReviewFinding(
                raw.getFilename(),
                raw.getLine(),
                severity,
                category,
                raw.getMessage(),
                raw.getSuggestion()
        );
    }

    private ReviewFindingSeverity parseSeverity(String severityStr) {
        if (severityStr == null || severityStr.isBlank()) {
            return ReviewFindingSeverity.INFO;
        }
        try {
            return ReviewFindingSeverity.valueOf(severityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReviewFindingSeverity.INFO;
        }
    }

    private ReviewFindingCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) {
            return ReviewFindingCategory.OTHER;
        }
        try {
            return ReviewFindingCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReviewFindingCategory.OTHER;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParsedReviewOutput {
        private String summary;
        private List<ParsedFinding> findings;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<ParsedFinding> getFindings() {
            return findings;
        }

        public void setFindings(List<ParsedFinding> findings) {
            this.findings = findings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParsedFinding {
        private String filename;
        private Integer line;
        private String severity;
        private String category;
        private String message;
        private String suggestion;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Integer getLine() {
            return line;
        }

        public void setLine(Integer line) {
            this.line = line;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
