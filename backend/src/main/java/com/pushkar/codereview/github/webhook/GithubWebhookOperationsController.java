package com.pushkar.codereview.github.webhook;

import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryDetailResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliveryItemResponse;
import com.pushkar.codereview.github.webhook.dto.WebhookDeliverySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping({"/webhooks/deliveries", "/api/v1/webhooks/deliveries"})
public class GithubWebhookOperationsController {

    private final GithubWebhookService webhookService;

    public GithubWebhookOperationsController(GithubWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<Page<WebhookDeliveryItemResponse>> getDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        Page<WebhookDeliveryItemResponse> result = webhookService.getDeliveries(page, size, status, repository, from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<WebhookDeliverySummaryResponse> getSummary() {
        WebhookDeliverySummaryResponse summary = webhookService.getSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<WebhookDeliveryDetailResponse> getDeliveryDetail(@PathVariable String deliveryId) {
        WebhookDeliveryDetailResponse detail = webhookService.getDeliveryDetail(deliveryId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{deliveryId}/retry")
    public ResponseEntity<WebhookDeliveryItemResponse> retryDelivery(@PathVariable String deliveryId) {
        WebhookDeliveryItemResponse retried = webhookService.retryDelivery(deliveryId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(retried);
    }
}
