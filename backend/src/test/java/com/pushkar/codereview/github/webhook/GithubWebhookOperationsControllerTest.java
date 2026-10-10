package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryDetailResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryItemResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliverySummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GithubWebhookOperationsControllerTest {

    private MockMvc mockMvc;
    private StubOperationsWebhookService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubOperationsWebhookService();
        GithubWebhookOperationsController controller = new GithubWebhookOperationsController(stubService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetDeliveries_Returns200OKWithPage() throws Exception {
        WebhookDeliveryItemResponse item = new WebhookDeliveryItemResponse();
        item.setId(1L);
        item.setDeliveryId("deliv-101");
        item.setRepository("octocat/hello-world");
        item.setStatus(WebhookDeliveryStatus.COMPLETED);
        item.setCodeReviewId(55L);

        stubService.setDeliveriesPage(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/webhooks/deliveries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryId").value("deliv-101"))
                .andExpect(jsonPath("$.content[0].repository").value("octocat/hello-world"))
                .andExpect(jsonPath("$.content[0].codeReviewId").value(55));
    }

    @Test
    void testGetSummary_Returns200OKWithSummary() throws Exception {
        WebhookDeliverySummaryResponse summary = new WebhookDeliverySummaryResponse(10, 8, 1, 1, 0);
        stubService.setSummaryResponse(summary);

        mockMvc.perform(get("/api/v1/webhooks/deliveries/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeliveries").value(10))
                .andExpect(jsonPath("$.completedDeliveries").value(8))
                .andExpect(jsonPath("$.processingDeliveries").value(1))
                .andExpect(jsonPath("$.failedDeliveries").value(1));
    }

    @Test
    void testGetDeliveryDetail_ExistingDelivery_Returns200OK() throws Exception {
        WebhookDeliveryDetailResponse detail = new WebhookDeliveryDetailResponse();
        detail.setId(1L);
        detail.setDeliveryId("deliv-xyz");
        detail.setRepository("octocat/hello-world");
        detail.setStatus(WebhookDeliveryStatus.FAILED);
        detail.setRetryable(true);
        detail.setRetryEligible(true);
        detail.setErrorCategory("TRANSIENT");
        detail.setErrorMessage("Temporary network timeout");

        stubService.setDetailResponse(detail);

        mockMvc.perform(get("/api/v1/webhooks/deliveries/deliv-xyz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value("deliv-xyz"))
                .andExpect(jsonPath("$.errorCategory").value("TRANSIENT"))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    void testGetDeliveryDetail_NotFound_Returns404() throws Exception {
        stubService.setDetailException(new ResourceNotFoundException("Webhook delivery not found with ID: not-found"));

        mockMvc.perform(get("/api/v1/webhooks/deliveries/not-found")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testRetryDelivery_Eligible_Returns202Accepted() throws Exception {
        WebhookDeliveryItemResponse item = new WebhookDeliveryItemResponse();
        item.setId(2L);
        item.setDeliveryId("deliv-failed");
        item.setStatus(WebhookDeliveryStatus.COMPLETED);
        item.setCodeReviewId(77L);
        item.setAttemptCount(2);

        stubService.setRetryResponse(item);

        mockMvc.perform(post("/api/v1/webhooks/deliveries/deliv-failed/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.deliveryId").value("deliv-failed"))
                .andExpect(jsonPath("$.attemptCount").value(2));
    }

    @Test
    void testRetryDelivery_Ineligible_Returns400BadRequest() throws Exception {
        stubService.setRetryException(new IllegalArgumentException("Completed deliveries cannot be retried"));

        mockMvc.perform(post("/api/v1/webhooks/deliveries/deliv-completed/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Completed deliveries cannot be retried"));
    }

    @Test
    void testRetryDelivery_UnauthorizedOrNotFound_Returns404() throws Exception {
        stubService.setRetryException(new ResourceNotFoundException("Webhook delivery not found with ID: deliv-other"));

        mockMvc.perform(post("/api/v1/webhooks/deliveries/deliv-other/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Stub Operations Webhook Service ---

    private static class StubOperationsWebhookService extends GithubWebhookService {
        private Page<WebhookDeliveryItemResponse> deliveriesPage = Page.empty();
        private WebhookDeliverySummaryResponse summaryResponse = new WebhookDeliverySummaryResponse(0, 0, 0, 0, 0);
        private WebhookDeliveryDetailResponse detailResponse;
        private WebhookDeliveryItemResponse retryResponse;
        private RuntimeException detailException;
        private RuntimeException retryException;

        public StubOperationsWebhookService() {
            super(null, null, null, null, null, null);
        }

        public void setDeliveriesPage(Page<WebhookDeliveryItemResponse> page) {
            this.deliveriesPage = page;
        }

        public void setSummaryResponse(WebhookDeliverySummaryResponse summary) {
            this.summaryResponse = summary;
        }

        public void setDetailResponse(WebhookDeliveryDetailResponse detail) {
            this.detailResponse = detail;
        }

        public void setRetryResponse(WebhookDeliveryItemResponse retry) {
            this.retryResponse = retry;
        }

        public void setDetailException(RuntimeException ex) {
            this.detailException = ex;
        }

        public void setRetryException(RuntimeException ex) {
            this.retryException = ex;
        }

        @Override
        public Page<WebhookDeliveryItemResponse> getDeliveries(int page, int size, String status, String repository, Instant from, Instant to) {
            return deliveriesPage;
        }

        @Override
        public WebhookDeliverySummaryResponse getSummary() {
            return summaryResponse;
        }

        @Override
        public WebhookDeliveryDetailResponse getDeliveryDetail(String deliveryId) {
            if (detailException != null) throw detailException;
            if (detailResponse != null) return detailResponse;
            throw new ResourceNotFoundException("Not found: " + deliveryId);
        }

        @Override
        public WebhookDeliveryItemResponse retryDelivery(String deliveryId) {
            if (retryException != null) throw retryException;
            if (retryResponse != null) return retryResponse;
            WebhookDeliveryItemResponse defaultResp = new WebhookDeliveryItemResponse();
            defaultResp.setDeliveryId(deliveryId);
            defaultResp.setStatus(WebhookDeliveryStatus.COMPLETED);
            return defaultResp;
        }
    }
}
