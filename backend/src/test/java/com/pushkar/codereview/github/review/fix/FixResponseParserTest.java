package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixResponseParserTest {

    private FixResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new FixResponseParser();
    }

    @Test
    void testParseResponse_ValidJson_ReturnsCodeFixResponse() {
        String json = "{\n" +
                "  \"filePath\": \"src/main/Activity.java\",\n" +
                "  \"explanation\": \"Added null pointer check\",\n" +
                "  \"originalContent\": \"activity.run();\",\n" +
                "  \"proposedContent\": \"if (activity != null) activity.run();\",\n" +
                "  \"unifiedDiff\": \"--- a/src/main/Activity.java\\n+++ b/src/main/Activity.java\\n@@ -10,1 +10,1 @@\\n-activity.run();\\n+if (activity != null) activity.run();\",\n" +
                "  \"limitations\": \"None\"\n" +
                "}";

        CodeFixResponse response = parser.parseResponse(json, 42L, "src/main/Activity.java", "Gemini");

        assertThat(response).isNotNull();
        assertThat(response.getFindingId()).isEqualTo(42L);
        assertThat(response.getFilePath()).isEqualTo("src/main/Activity.java");
        assertThat(response.getExplanation()).isEqualTo("Added null pointer check");
        assertThat(response.getUnifiedDiff()).contains("-activity.run();");
        assertThat(response.getUnifiedDiff()).contains("+if (activity != null) activity.run();");
        assertThat(response.getStatus()).isEqualTo("PROPOSED");
        assertThat(response.getProvider()).isEqualTo("Gemini");
    }

    @Test
    void testParseResponse_WrappedInMarkdownCodeBlock_ParsesCorrectly() {
        String wrapped = "```json\n" +
                "{\n" +
                "  \"filePath\": \"src/main/Activity.java\",\n" +
                "  \"explanation\": \"Safe check added\",\n" +
                "  \"unifiedDiff\": \"@@ -1 +1 @@\\n-old\\n+new\"\n" +
                "}\n" +
                "```";

        CodeFixResponse response = parser.parseResponse(wrapped, 42L, "src/main/Activity.java", "Gemini");

        assertThat(response).isNotNull();
        assertThat(response.getUnifiedDiff()).contains("--- a/src/main/Activity.java");
        assertThat(response.getUnifiedDiff()).contains("-old");
        assertThat(response.getUnifiedDiff()).contains("+new");
    }

    @Test
    void testParseResponse_NullOrEmpty_ThrowsException() {
        assertThatThrownBy(() -> parser.parseResponse(null, 42L, "path", "Gemini"))
                .isInstanceOf(GeminiAiReviewException.class);

        assertThatThrownBy(() -> parser.parseResponse("   ", 42L, "path", "Gemini"))
                .isInstanceOf(GeminiAiReviewException.class);
    }

    @Test
    void testParseResponse_MalformedJson_ThrowsException() {
        assertThatThrownBy(() -> parser.parseResponse("invalid json text", 42L, "path", "Gemini"))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Failed to parse Gemini response");
    }
}
