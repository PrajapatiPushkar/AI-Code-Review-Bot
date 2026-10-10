package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.config.GithubProperties;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.github.webhook.dto.GithubWebhookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GithubWebhookControllerTest {

    private MockMvc mockMvc;
    private GithubProperties githubProperties;
    private GithubWebhookSignatureVerifier signatureVerifier;
    private StubWebhookService stubWebhookService;

    @BeforeEach
    void setUp() {
        githubProperties = new GithubProperties();
        githubProperties.setWebhookSecret("test-secret-123");
        signatureVerifier = new GithubWebhookSignatureVerifier(githubProperties);
        stubWebhookService = new StubWebhookService();

        GithubWebhookController controller = new GithubWebhookController(stubWebhookService, signatureVerifier);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testHandleWebhook_ValidSignatureAndHeaders_Returns202Accepted() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);
        stubWebhookService.setResponseToReturn(GithubWebhookResponse.accepted("deliv-1", 42L, "Review queued"));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value("deliv-1"))
                .andExpect(jsonPath("$.reviewId").value(42))
                .andExpect(jsonPath("$.initiated").value(true));
    }

    @Test
    void testHandleWebhook_LegacyRoute_Returns202Accepted() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);
        stubWebhookService.setResponseToReturn(GithubWebhookResponse.accepted("deliv-legacy", 43L, "Review queued"));

        mockMvc.perform(post("/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-legacy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value("deliv-legacy"));
    }

    @Test
    void testHandleWebhook_MissingSignature_Returns401Unauthorized() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-no-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Missing X-Hub-Signature-256 header"));
    }

    @Test
    void testHandleWebhook_InvalidSignature_Returns401Unauthorized() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=invalidhash1234567890abcdef")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-bad-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"));
    }

    @Test
    void testHandleWebhook_UnconfiguredSecret_Returns401Unauthorized() throws Exception {
        githubProperties.setWebhookSecret("");
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=something")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-no-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Webhook secret is not configured"));
    }

    @Test
    void testHandleWebhook_MissingEventHeader_Returns400BadRequest() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Delivery", "deliv-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required X-GitHub-Event header"));
    }

    @Test
    void testHandleWebhook_MissingDeliveryHeader_Returns400BadRequest() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required X-GitHub-Delivery header"));
    }

    @Test
    void testHandleWebhook_MissingBody_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=abcdef")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-no-body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing or empty request body"));
    }

    @Test
    void testHandleWebhook_PingEvent_Returns200OK() throws Exception {
        byte[] payload = "{\"zen\":\"Mindfulness\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);
        stubWebhookService.setResponseToReturn(GithubWebhookResponse.pong("deliv-ping", "Ping acknowledged"));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "ping")
                        .header("X-GitHub-Delivery", "deliv-ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PONG"))
                .andExpect(jsonPath("$.message").value("Ping acknowledged"));
    }

    @Test
    void testHandleWebhook_DuplicateDelivery_Returns200OK() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = signatureVerifier.computeSignature(payload);
        stubWebhookService.setResponseToReturn(GithubWebhookResponse.duplicate("deliv-dup", 99L, "Already processed"));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "deliv-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.reviewId").value(99))
                .andExpect(jsonPath("$.initiated").value(false));
    }

    // --- Stub Helper ---

    private static class StubWebhookService extends GithubWebhookService {
        private GithubWebhookResponse responseToReturn;

        public StubWebhookService() {
            super(null, null, null, null, null);
        }

        public void setResponseToReturn(GithubWebhookResponse response) {
            this.responseToReturn = response;
        }

        @Override
        public GithubWebhookResponse processWebhook(String deliveryId, String eventType, byte[] payloadBytes) {
            if (responseToReturn != null) {
                return responseToReturn;
            }
            return GithubWebhookResponse.accepted(deliveryId, 1L, "Default accepted");
        }
    }
}
