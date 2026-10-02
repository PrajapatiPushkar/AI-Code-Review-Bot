package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAiReviewEngineTest {

    private static final String API_KEY = "test-secret-api-key-99999";
    private static final String MODEL = "gemini-3.6-flash";
    private static final String FALLBACK_MODEL = "gemini-3.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private GeminiProperties geminiProperties;
    private ReviewPromptBuilder promptBuilder;
    private GeminiResponseParser responseParser;
    private GeminiAiReviewEngine engine;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties(API_KEY, MODEL, FALLBACK_MODEL, BASE_URL);
        promptBuilder = new ReviewPromptBuilder();
        responseParser = new GeminiResponseParser();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        engine = new GeminiAiReviewEngine(builder, geminiProperties, promptBuilder, responseParser);
    }

    @Test
    void testReview_Success() {
        ReviewInput input = new ReviewInput(
                1L, "repo", "owner/repo", "https://github.com/owner/repo", "main",
                10L, 1L, "Fix bug", "Fix null pointer", "open",
                "https://github.com/owner/repo/pull/1", "octocat", "feature", "main",
                null, null, Collections.emptyList()
        );

        String geminiResponseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\": \\"Good PR\\", \\"findings\\": [{\\"filename\\": \\"Main.java\\", \\"line\\": 10, \\"severity\\": \\"HIGH\\", \\"category\\": \\"BUG\\", \\"message\\": \\"Issue\\", \\"suggestion\\": \\"Fix\\"}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(geminiResponseJson, MediaType.APPLICATION_JSON));

        ReviewResult result = engine.review(input);

        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Good PR");
        assertThat(result.getFindings()).hasSize(1);
        assertThat(result.getFindings().get(0).getFilename()).isEqualTo("Main.java");
        assertThat(result.getFindings().get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(result.getFindings().get(0).getCategory()).isEqualTo(ReviewFindingCategory.BUG);
    }

    @Test
    void testEndpointConstructionAndMethod() {
        ReviewInput input = new ReviewInput(
                1L, "repo", "owner/repo", "https://github.com/owner/repo", "main",
                10L, 1L, "Fix bug", "Fix null pointer", "open",
                "https://github.com/owner/repo/pull/1", "octocat", "feature", "main",
                null, null, Collections.emptyList()
        );

        String expectedUri = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"OK\\\",\\\"findings\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

        engine.review(input);
        mockServer.verify();
    }

    @Test
    void testReview_NullInputThrowsException() {
        assertThatThrownBy(() -> engine.review(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReviewInput must not be null");
    }

    @Test
    void testReview_MissingApiKeyThrowsException() {
        geminiProperties.setApiKey(null);
        ReviewInput input = new ReviewInput();

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Gemini API key is missing or not configured");
    }

    @Test
    void testReview_Http400BadRequest_DoesNotTriggerFallback() {
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 400")
                .hasMessageNotContaining(API_KEY);

        mockServer.verify();
    }

    @Test
    void testReview_Http401Unauthorized_DoesNotTriggerFallback() {
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 401")
                .hasMessageNotContaining(API_KEY);

        mockServer.verify();
    }

    @Test
    void testReview_PrimaryReturns503_FallbackSucceeds() {
        ReviewInput input = new ReviewInput();

        // 1st request to primary model fails with 503
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Fallback request to fallback model succeeds
        String fallbackResponseJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Fallback Success\\\",\\\"findings\\\":[]}\"}]}}]}";
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withSuccess(fallbackResponseJson, MediaType.APPLICATION_JSON));

        ReviewResult result = engine.review(input);

        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Fallback Success");
    }

    @Test
    void testReview_PrimaryModelRetriesBeforeFallback() {
        com.pushkar.codereview.config.resilience.ResilienceProperties props = new com.pushkar.codereview.config.resilience.ResilienceProperties();
        props.getRetry().setMaxAttempts(2);
        props.getRetry().setInitialIntervalMs(10);
        com.pushkar.codereview.config.resilience.ResilienceExecutor resExecutor = new com.pushkar.codereview.config.resilience.ResilienceExecutor(props, null);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAiReviewEngine resilientEngine = new GeminiAiReviewEngine(builder, geminiProperties, promptBuilder, responseParser, null, resExecutor);

        ReviewInput input = new ReviewInput();

        // Primary attempt 1: 503
        server.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Primary attempt 2 (retry): 503
        server.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Fallback attempt 1: 200 OK
        server.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Recovered with fallback\\\",\\\"findings\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

        ReviewResult result = resilientEngine.review(input);
        server.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Recovered with fallback");
    }

    @Test
    void testReview_PrimaryAndFallbackBothFail() {
        ReviewInput input = new ReviewInput();

        // Primary fails with 503
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Fallback also fails with 503
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 503")
                .hasMessageNotContaining(API_KEY);

        mockServer.verify();
    }

    @Test
    void testReview_FallbackDoesNotCreateInfiniteLoop_WhenFallbackFails() {
        ReviewInput input = new ReviewInput();

        // Primary fails with 503
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Fallback fails with 500
        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 500");

        // mockServer.verify() ensures NO extra calls were made
        mockServer.verify();
    }

    @Test
    void testReview_FallbackDoesNotTrigger_WhenFallbackModelMatchesPrimary() {
        geminiProperties.setFallbackModel(MODEL);
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 503");

        mockServer.verify();
    }

    @Test
    void testReview_FallbackDoesNotTrigger_WhenFallbackModelIsNull() {
        geminiProperties.setFallbackModel(null);
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("status code: 503");

        mockServer.verify();
    }

    @Test
    void testReview_Http429TooManyRequests_FallbackSucceeds() {
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Recovered from 429\\\",\\\"findings\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

        ReviewResult result = engine.review(input);

        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Recovered from 429");
    }

    @Test
    void testReview_Http500InternalServerError_FallbackSucceeds() {
        ReviewInput input = new ReviewInput();

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + FALLBACK_MODEL + ":generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Recovered from 500\\\",\\\"findings\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

        ReviewResult result = engine.review(input);

        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Recovered from 500");
    }

    @Test
    void testReview_MalformedAiResponseText_DoesNotTriggerFallback() {
        ReviewInput input = new ReviewInput();

        String malformedGeminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "This is raw unformatted text without any JSON"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withSuccess(malformedGeminiResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> engine.review(input))
                .isInstanceOf(GeminiAiReviewException.class)
                .hasMessageContaining("Failed to parse Gemini review response into ReviewResult")
                .hasMessageNotContaining(API_KEY);

        mockServer.verify();
    }

    @Test
    void testReview_WithResilienceExecutor_Transient503SucceedsOnRetry() {
        com.pushkar.codereview.config.resilience.ResilienceProperties props = new com.pushkar.codereview.config.resilience.ResilienceProperties();
        props.getRetry().setMaxAttempts(2);
        props.getRetry().setInitialIntervalMs(10);
        com.pushkar.codereview.config.resilience.ResilienceExecutor resExecutor = new com.pushkar.codereview.config.resilience.ResilienceExecutor(props, null);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiAiReviewEngine resilientEngine = new GeminiAiReviewEngine(builder, geminiProperties, promptBuilder, responseParser, null, resExecutor);

        ReviewInput input = new ReviewInput();

        // 1st attempt: 503 Service Unavailable
        server.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // 2nd attempt: Success on primary
        server.expect(requestTo(BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Recovered after 503 on retry\\\",\\\"findings\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

        ReviewResult result = resilientEngine.review(input);
        server.verify();
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Recovered after 503 on retry");
    }
}
