package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.qualitygate.dto.QualityGateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/code-reviews", "/api/v1/code-reviews"})
public class QualityGateController {

    private final QualityGateService qualityGateService;

    public QualityGateController(QualityGateService qualityGateService) {
        this.qualityGateService = qualityGateService;
    }

    @GetMapping("/{reviewId}/quality-gate")
    public ResponseEntity<QualityGateResponse> getQualityGate(@PathVariable Long reviewId) {
        QualityGateResponse response = qualityGateService.getOrEvaluateQualityGate(reviewId);
        return ResponseEntity.ok(response);
    }
}
