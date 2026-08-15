package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentRequest;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentResponse;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class GeminiAiReviewEngine implements AiReviewEngine {

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ReviewPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    public GeminiAiReviewEngine(RestClient.Builder restClientBuilder,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.restClient = restClientBuilder
                .baseUrl(geminiProperties.getApiBaseUrl())
                .build();
    }

    public GeminiAiReviewEngine(RestClient restClient,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    @Override
    public ReviewResult review(ReviewInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ReviewInput must not be null");
        }

        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAiReviewException("Gemini API key is missing or not configured");
        }

        String prompt = promptBuilder.buildPrompt(input);
        GeminiGenerateContentRequest requestPayload = GeminiGenerateContentRequest.fromText(prompt);

        String uriPath = "/v1beta/models/" + geminiProperties.getModel() + ":generateContent";

        try {
            GeminiGenerateContentResponse response = restClient.post()
                    .uri(uriPath)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);

            if (response == null) {
                throw new GeminiAiReviewException("Received null response from Gemini API");
            }

            String text = response.getFirstCandidateText();
            if (text == null || text.isBlank()) {
                throw new GeminiAiReviewException("Gemini API returned an empty or candidate-less response");
            }

            return responseParser.parseResponse(text);

        } catch (GeminiAiReviewException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new GeminiAiReviewException("Gemini API HTTP request failed with status code: " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new GeminiAiReviewException("Failed to execute Gemini AI review request", e);
        }
    }
}
