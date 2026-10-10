package com.pushkar.codereview.analytics;

import com.pushkar.codereview.analytics.dto.AnalyticsFindingsResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsOverviewResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsTrendItemResponse;
import com.pushkar.codereview.analytics.dto.RepositoryHealthSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/analytics", "/api/v1/analytics"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String owner
    ) {
        AnalyticsOverviewResponse response = analyticsService.getOverview(from, to, repository, owner);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/findings")
    public ResponseEntity<AnalyticsFindingsResponse> getFindings(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String owner
    ) {
        AnalyticsFindingsResponse response = analyticsService.getFindings(from, to, repository, owner);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trends")
    public ResponseEntity<List<AnalyticsTrendItemResponse>> getTrends(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String owner
    ) {
        List<AnalyticsTrendItemResponse> response = analyticsService.getTrends(from, to, repository, owner);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositoryHealthSummaryResponse>> getRepositories(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String owner
    ) {
        List<RepositoryHealthSummaryResponse> response = analyticsService.getRepositorySummaries(from, to, repository, owner);
        return ResponseEntity.ok(response);
    }
}
