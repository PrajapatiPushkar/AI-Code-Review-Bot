package com.pushkar.codereview.github.review.ai;

import com.pushkar.codereview.config.CodeReviewMetrics;
import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentRequest;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentResponse;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class GeminiAiReviewEngine implements AiReviewEngine {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiReviewEngine.class);

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ReviewPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    private final CodeReviewMetrics codeReviewMetrics;

    public GeminiAiReviewEngine(RestClient.Builder restClientBuilder,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this(restClientBuilder, geminiProperties, promptBuilder, responseParser, null);
    }

    public GeminiAiReviewEngine(RestClient.Builder restClientBuilder,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser,
                                @org.springframework.beans.factory.annotation.Autowired(required = false) CodeReviewMetrics codeReviewMetrics) {
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.codeReviewMetrics = codeReviewMetrics;
        this.restClient = restClientBuilder
                .baseUrl(geminiProperties.getApiBaseUrl())
                .build();
    }

    public GeminiAiReviewEngine(RestClient restClient,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this(restClient, geminiProperties, promptBuilder, responseParser, null);
    }

    public GeminiAiReviewEngine(RestClient restClient,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser,
                                CodeReviewMetrics codeReviewMetrics) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.codeReviewMetrics = codeReviewMetrics;
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

        int filesCount = (input.getFiles() != null) ? input.getFiles().size() : 0;
        log.info("Executing Gemini AI code review request for model={} (filesCount={})", geminiProperties.getModel(), filesCount);

        String prompt = promptBuilder.buildPrompt(input);
        GeminiGenerateContentRequest requestPayload = GeminiGenerateContentRequest.fromText(prompt);

        String uriPath = "/v1beta/models/" + geminiProperties.getModel() + ":generateContent";

        long startTime = System.currentTimeMillis();

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

            ReviewResult result = responseParser.parseResponse(text);
            long duration = System.currentTimeMillis() - startTime;
            if (codeReviewMetrics != null) {
                codeReviewMetrics.recordAiExecutionTime(duration);
            }

            int findingsCount = (result != null && result.getFindings() != null) ? result.getFindings().size() : 0;
            log.info("Gemini AI review execution completed successfully with {} findings in {} ms", findingsCount, duration);
            return result;

        } catch (GeminiAiReviewException e) {
            log.error("Gemini AI code review failed: {}", e.getMessage());
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("Gemini API HTTP request failed with status code {}: {}", e.getStatusCode().value(), e.getMessage());
            throw new GeminiAiReviewException("Gemini API HTTP request failed with status code: " + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("Failed to execute Gemini AI review request: {}", e.getMessage(), e);
            throw new GeminiAiReviewException("Failed to execute Gemini AI review request", e);
        }
    }
}
