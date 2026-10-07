package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.config.GeminiProperties;
import com.pushkar.codereview.config.resilience.ResilienceExecutor;
import com.pushkar.codereview.exception.GeminiAiReviewException;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentRequest;
import com.pushkar.codereview.github.review.ai.gemini.dto.GeminiGenerateContentResponse;
import com.pushkar.codereview.github.review.dto.CodeFixResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class GeminiAiFixEngine implements AiFixEngine {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiFixEngine.class);

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final FixPromptBuilder promptBuilder;
    private final FixResponseParser responseParser;
    private final PatchValidator patchValidator;
    private final ResilienceExecutor resilienceExecutor;

    public GeminiAiFixEngine(RestClient.Builder restClientBuilder,
                             GeminiProperties geminiProperties,
                             FixPromptBuilder promptBuilder,
                             FixResponseParser responseParser,
                             PatchValidator patchValidator) {
        this(restClientBuilder, geminiProperties, promptBuilder, responseParser, patchValidator, null);
    }

    @Autowired
    public GeminiAiFixEngine(RestClient.Builder restClientBuilder,
                             GeminiProperties geminiProperties,
                             FixPromptBuilder promptBuilder,
                             FixResponseParser responseParser,
                             PatchValidator patchValidator,
                             @Autowired(required = false) ResilienceExecutor resilienceExecutor) {
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.patchValidator = patchValidator;
        this.resilienceExecutor = resilienceExecutor;
        this.restClient = restClientBuilder
                .baseUrl(geminiProperties.getApiBaseUrl())
                .build();
    }

    public GeminiAiFixEngine(RestClient restClient,
                             GeminiProperties geminiProperties,
                             FixPromptBuilder promptBuilder,
                             FixResponseParser responseParser,
                             PatchValidator patchValidator,
                             ResilienceExecutor resilienceExecutor) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.patchValidator = patchValidator;
        this.resilienceExecutor = resilienceExecutor;
    }

    @Override
    public CodeFixResponse generateFix(FixGenerationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("FixGenerationInput must not be null");
        }

        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAiReviewException("Gemini API key is missing or not configured. Please set GEMINI_API_KEY in your .env file.");
        }

        String primaryModel = geminiProperties.getModel();
        String fallbackModel = geminiProperties.getFallbackModel();
        String correlationId = MDC.get("correlationId");

        try {
            return executeModelWithResilience(primaryModel, input, apiKey);
        } catch (Exception primaryEx) {
            boolean canFallback = fallbackModel != null
                    && !fallbackModel.isBlank()
                    && !fallbackModel.equalsIgnoreCase(primaryModel)
                    && isFallbackEligible(primaryEx);

            if (!canFallback) {
                throw primaryEx;
            }

            log.warn("Primary Gemini model unavailable for fix generation; falling back from {} to {} [correlationId={}]",
                    primaryModel, fallbackModel, correlationId);

            try {
                return executeModelWithResilience(fallbackModel, input, apiKey);
            } catch (Exception fallbackEx) {
                log.error("Fallback Gemini model {} also failed for fix generation [correlationId={}]: {}",
                        fallbackModel, correlationId, fallbackEx.getMessage());
                throw fallbackEx;
            }
        }
    }

    private CodeFixResponse executeModelWithResilience(String model, FixGenerationInput input, String apiKey) {
        String correlationId = MDC.get("correlationId");
        log.info("Generating proposed fix with model={} for findingId={} [correlationId={}]",
                model, input.getFindingId(), correlationId);

        String prompt = promptBuilder.buildPrompt(input);
        GeminiGenerateContentRequest requestPayload = GeminiGenerateContentRequest.fromText(prompt);
        String uriPath = "/v1beta/models/" + model + ":generateContent";

        CodeFixResponse response;
        if (resilienceExecutor != null) {
            response = resilienceExecutor.executeSupplier("gemini", () -> executeGeminiRequest(uriPath, apiKey, requestPayload, input, model));
        } else {
            response = executeGeminiRequest(uriPath, apiKey, requestPayload, input, model);
        }

        // Enforce patch validation
        patchValidator.validate(input.getFilePath(), response.getFilePath(), response.getUnifiedDiff());
        return response;
    }

    private CodeFixResponse executeGeminiRequest(String uriPath, String apiKey, GeminiGenerateContentRequest requestPayload,
                                                 FixGenerationInput input, String model) {
        try {
            GeminiGenerateContentResponse response = restClient.post()
                    .uri(uriPath)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);

            if (response == null) {
                throw new GeminiAiReviewException("Received null response from Gemini API during fix generation");
            }

            String text = response.getFirstCandidateText();
            if (text == null || text.isBlank()) {
                throw new GeminiAiReviewException("Gemini API returned an empty or candidate-less fix response");
            }

            String provider = "Gemini (" + model + ")";
            return responseParser.parseResponse(text, input.getFindingId(), input.getFilePath(), provider);

        } catch (GeminiAiReviewException e) {
            log.error("Gemini fix generation failed for model={}: {}", model, e.getMessage());
            throw e;
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            log.error("Gemini API HTTP request failed during fix generation for model={} with status {}: {}",
                    model, statusCode, e.getStatusText());
            throw new GeminiAiReviewException("Gemini API HTTP request failed with status code: " + statusCode, statusCode, e);
        } catch (Exception e) {
            log.error("Failed to execute Gemini fix generation for model={}: {}", model, e.getMessage(), e);
            throw new GeminiAiReviewException("Failed to execute Gemini fix generation request", e);
        }
    }

    private boolean isFallbackEligible(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof org.springframework.security.access.AccessDeniedException
                || throwable instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            return false;
        }

        if (throwable instanceof GeminiAiReviewException gare) {
            String msg = gare.getMessage() != null ? gare.getMessage().toLowerCase() : "";
            if (msg.contains("missing") || msg.contains("invalid") || msg.contains("not configured")
                    || msg.contains("status code: 400") || msg.contains("status code: 401") || msg.contains("status code: 403")) {
                return false;
            }
            if (gare.getStatusCode() != null) {
                int code = gare.getStatusCode();
                return code == 503 || code == 502 || code == 504 || code == 429 || code >= 500;
            }
            return msg.contains("503") || msg.contains("502") || msg.contains("504") || msg.contains("429")
                    || msg.contains("timeout") || msg.contains("unavailable") || msg.contains("high demand");
        }

        if (throwable instanceof HttpStatusCodeException httpEx) {
            int code = httpEx.getStatusCode().value();
            return code == 503 || code == 502 || code == 504 || code == 429 || code >= 500;
        }

        return throwable instanceof org.springframework.web.client.ResourceAccessException
                || throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.ConnectException;
    }
}
