package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiResponseParserTest {

    private GeminiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new GeminiResponseParser();
    }

    @Test
    void testParseResponse_ValidPlainJson() {
        String json = """
                {
                  "summary": "Looks good overall with minor bug.",
                  "findings": [
                    {
                      "filename": "src/App.java",
                      "line": 42,
                      "severity": "HIGH",
                      "category": "BUG",
                      "message": "Potential NullPointerException on line 42",
                      "suggestion": "Add null check before method call"
                    }
                  ]
                }
                """;

        ReviewResult result = parser.parseResponse(json);

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Looks good overall with minor bug.");
        assertThat(result.getFindings()).hasSize(1);
        assertThat(result.getFindings().get(0).getFilename()).isEqualTo("src/App.java");
        assertThat(result.getFindings().get(0).getLine()).isEqualTo(42);
        assertThat(result.getFindings().get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(result.getFindings().get(0).getCategory()).isEqualTo(ReviewFindingCategory.BUG);
        assertThat(result.getFindings().get(0).getMessage()).isEqualTo("Potential NullPointerException on line 42");
        assertThat(result.getFindings().get(0).getSuggestion()).isEqualTo("Add null check before method call");
    }

    @Test
    void testParseResponse_JsonInMarkdownFences() {
        String markdown = """
                ```json
                {
                  "summary": "Code review complete.",
                  "findings": []
                }
                ```
                """;

        ReviewResult result = parser.parseResponse(markdown);

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Code review complete.");
        assertThat(result.getFindings()).isNotNull().isEmpty();
    }

    @Test
    void testParseResponse_MultipleFindingsAndNullLine() {
        String json = """
                {
                  "summary": "Multiple findings identified.",
                  "findings": [
                    {
                      "filename": "src/App.java",
                      "line": null,
                      "severity": "CRITICAL",
                      "category": "SECURITY",
                      "message": "Hardcoded secret detected",
                      "suggestion": "Move secret to environment variables"
                    },
                    {
                      "filename": "src/Utils.java",
                      "line": 15,
                      "severity": "LOW",
                      "category": "CODE_STYLE",
                      "message": "Unused variable",
                      "suggestion": "Remove unused variable"
                    }
                  ]
                }
                """;

        ReviewResult result = parser.parseResponse(json);

        assertThat(result.getFindings()).hasSize(2);
        assertThat(result.getFindings().get(0).getLine()).isNull();
        assertThat(result.getFindings().get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.CRITICAL);
        assertThat(result.getFindings().get(0).getCategory()).isEqualTo(ReviewFindingCategory.SECURITY);

        assertThat(result.getFindings().get(1).getLine()).isEqualTo(15);
        assertThat(result.getFindings().get(1).getSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
        assertThat(result.getFindings().get(1).getCategory()).isEqualTo(ReviewFindingCategory.CODE_STYLE);
    }

    @Test
    void testParseResponse_InvalidSeverityAndCategoryFallbackSafely() {
        String json = """
                {
                  "summary": "Review summary",
                  "findings": [
                    {
                      "filename": "src/App.java",
                      "line": 10,
                      "severity": "UNKNOWN_SEVERITY",
                      "category": "UNKNOWN_CATEGORY",
                      "message": "Some message",
                      "suggestion": "Some suggestion"
                    }
                  ]
                }
                """;

        ReviewResult result = parser.parseResponse(json);

        assertThat(result.getFindings()).hasSize(1);
        assertThat(result.getFindings().get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.INFO);
        assertThat(result.getFindings().get(0).getCategory()).isEqualTo(ReviewFindingCategory.OTHER);
    }

    @Test
    void testParseResponse_MalformedJsonThrowsGeminiAiReviewException() {
        String malformedJson = "{ summary: 'incomplete json without closing braces";

        assertThatThrownBy(() -> parser.parseResponse(malformedJson))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Failed to parse Gemini review response into ReviewResult");
    }

    @Test
    void testParseResponse_NullOrBlankResponseThrowsException() {
        assertThatThrownBy(() -> parser.parseResponse(null))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Gemini response text is null or empty");

        assertThatThrownBy(() -> parser.parseResponse("   "))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Gemini response text is null or empty");
    }
}
