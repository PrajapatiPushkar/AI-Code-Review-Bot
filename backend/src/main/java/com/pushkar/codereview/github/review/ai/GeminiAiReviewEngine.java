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
    private final com.pushkar.codereview.config.resilience.ResilienceExecutor resilienceExecutor;

    public GeminiAiReviewEngine(RestClient.Builder restClientBuilder,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this(restClientBuilder, geminiProperties, promptBuilder, responseParser, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiAiReviewEngine(RestClient.Builder restClientBuilder,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser,
                                @org.springframework.beans.factory.annotation.Autowired(required = false) CodeReviewMetrics codeReviewMetrics,
                                @org.springframework.beans.factory.annotation.Autowired(required = false) com.pushkar.codereview.config.resilience.ResilienceExecutor resilienceExecutor) {
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.codeReviewMetrics = codeReviewMetrics;
        this.resilienceExecutor = resilienceExecutor;
        this.restClient = restClientBuilder
                .baseUrl(geminiProperties.getApiBaseUrl())
                .build();
    }

    public GeminiAiReviewEngine(RestClient restClient,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser) {
        this(restClient, geminiProperties, promptBuilder, responseParser, null, null);
    }

    public GeminiAiReviewEngine(RestClient restClient,
                                GeminiProperties geminiProperties,
                                ReviewPromptBuilder promptBuilder,
                                GeminiResponseParser responseParser,
                                CodeReviewMetrics codeReviewMetrics,
                                com.pushkar.codereview.config.resilience.ResilienceExecutor resilienceExecutor) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.codeReviewMetrics = codeReviewMetrics;
        this.resilienceExecutor = resilienceExecutor;
    }

    @Override
    public ReviewResult review(ReviewInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ReviewInput must not be null");
        }

        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAiReviewException("Gemini API key is missing or not configured. Please set GEMINI_API_KEY in your .env file.");
        }

        int filesCount = (input.getFiles() != null) ? input.getFiles().size() : 0;
        String primaryModel = geminiProperties.getModel();
        String fallbackModel = geminiProperties.getFallbackModel();
        String reviewId = org.slf4j.MDC.get("reviewId");
        String correlationId = org.slf4j.MDC.get("correlationId");

        try {
            return executeModelWithResilience(primaryModel, input, apiKey, filesCount);
        } catch (Exception primaryEx) {
            boolean canFallback = fallbackModel != null
                    && !fallbackModel.isBlank()
                    && !fallbackModel.equalsIgnoreCase(primaryModel)
                    && isFallbackEligible(primaryEx);

            if (!canFallback) {
                throw primaryEx;
            }

            log.warn("Primary Gemini model unavailable; switching from {} to {} [reviewId={}, correlationId={}]",
                    primaryModel, fallbackModel, reviewId, correlationId);

            try {
                return executeModelWithResilience(fallbackModel, input, apiKey, filesCount);
            } catch (Exception fallbackEx) {
                log.error("Fallback Gemini model {} also failed [reviewId={}, correlationId={}]: {}",
                        fallbackModel, reviewId, correlationId, fallbackEx.getMessage());
                throw fallbackEx;
            }
        }
    }

    private ReviewResult executeModelWithResilience(String model, ReviewInput input, String apiKey, int filesCount) {
        String reviewId = org.slf4j.MDC.get("reviewId");
        String correlationId = org.slf4j.MDC.get("correlationId");
        log.info("Executing Gemini AI code review request for model={} (filesCount={}, reviewId={}, correlationId={})",
                model, filesCount, reviewId, correlationId);

        String prompt = promptBuilder.buildPrompt(input);
        GeminiGenerateContentRequest requestPayload = GeminiGenerateContentRequest.fromText(prompt);
        String uriPath = "/v1beta/models/" + model + ":generateContent";
        long startTime = System.currentTimeMillis();

        if (resilienceExecutor != null) {
            return resilienceExecutor.executeSupplier("gemini", () -> executeGeminiRequest(uriPath, apiKey, requestPayload, startTime, model));
        } else {
            return executeGeminiRequest(uriPath, apiKey, requestPayload, startTime, model);
        }
    }

    private ReviewResult executeGeminiRequest(String uriPath, String apiKey, GeminiGenerateContentRequest requestPayload, long startTime, String model) {
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
            log.info("Gemini AI review execution completed successfully for model={} with {} findings in {} ms",
                    model, findingsCount, duration);
            return result;

        } catch (GeminiAiReviewException e) {
            String reviewId = org.slf4j.MDC.get("reviewId");
            String correlationId = org.slf4j.MDC.get("correlationId");
            String cbState = getCircuitBreakerState();
            String status = e.getStatusCode() != null ? String.valueOf(e.getStatusCode()) : "N/A";
            log.error("Gemini AI code review failed for model={} [reviewId={}, correlationId={}, cbState={}, status={}, exceptionType={}]: {}",
                    model, reviewId, correlationId, cbState, status, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            String reviewId = org.slf4j.MDC.get("reviewId");
            String correlationId = org.slf4j.MDC.get("correlationId");
            String cbState = getCircuitBreakerState();
            log.error("Gemini API HTTP request failed for model={} with status code {} [reviewId={}, correlationId={}, cbState={}, exceptionType={}]: {}",
                    model, statusCode, reviewId, correlationId, cbState, e.getClass().getSimpleName(), e.getStatusText());
            throw new GeminiAiReviewException("Gemini API HTTP request failed with status code: " + statusCode, statusCode, e);
        } catch (Exception e) {
            String reviewId = org.slf4j.MDC.get("reviewId");
            String correlationId = org.slf4j.MDC.get("correlationId");
            String cbState = getCircuitBreakerState();
            log.error("Failed to execute Gemini AI review request for model={} [reviewId={}, correlationId={}, cbState={}, exceptionType={}]: {}",
                    model, reviewId, correlationId, cbState, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new GeminiAiReviewException("Failed to execute Gemini AI review request", e);
        }
    }

    public boolean isFallbackEligible(Throwable throwable) {
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
            if (isExplicitNonFallbackMessage(msg)) {
                return false;
            }
            if (gare.getStatusCode() != null) {
                return isTransientStatusCode(gare.getStatusCode());
            }
            if (gare.getCause() != null && gare.getCause() != gare) {
                return isFallbackEligible(gare.getCause());
            }
            return msg.contains("503") || msg.contains("502") || msg.contains("504")
                    || msg.contains("429") || msg.contains("500")
                    || msg.contains("timeout") || msg.contains("unavailable") || msg.contains("timed out")
                    || msg.contains("high demand");
        }

        if (throwable instanceof HttpStatusCodeException httpEx) {
            return isTransientStatusCode(httpEx.getStatusCode().value());
        }

        if (throwable instanceof org.springframework.web.client.ResourceAccessException
                || throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.ConnectException
                || (throwable instanceof java.io.IOException && !(throwable instanceof com.fasterxml.jackson.core.JsonProcessingException))) {
            return true;
        }

        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            return isFallbackEligible(throwable.getCause());
        }

        return false;
    }

    private boolean isTransientStatusCode(int status) {
        return status == 503 || status == 502 || status == 504 || status == 429 || status >= 500;
    }

    private boolean isExplicitNonFallbackMessage(String msg) {
        return msg.contains("missing") || msg.contains("invalid") || msg.contains("not configured")
                || msg.contains("candidate-less") || msg.contains("empty") || msg.contains("failed to parse")
                || msg.contains("null response") || msg.contains("status code: 400")
                || msg.contains("status code: 401") || msg.contains("status code: 403")
                || msg.contains("status code: 404");
    }

    private String getCircuitBreakerState() {
        if (resilienceExecutor != null) {
            io.github.resilience4j.circuitbreaker.CircuitBreaker cb = resilienceExecutor.getCircuitBreaker("gemini");
            if (cb != null) {
                return cb.getState().name();
            }
        }
        return "N/A";
    }
}
