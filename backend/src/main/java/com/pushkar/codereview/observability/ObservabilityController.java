package com.pushkar.codereview.observability;

import com.pushkar.codereview.observability.dto.ObservabilitySummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/observability", "/api/v1/observability"})
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping({"/metrics", "/summary"})
    public ResponseEntity<ObservabilitySummaryDto> getObservabilitySummary() {
        ObservabilitySummaryDto summary = observabilityService.getObservabilitySummary();
        return ResponseEntity.ok(summary);
    }
}
