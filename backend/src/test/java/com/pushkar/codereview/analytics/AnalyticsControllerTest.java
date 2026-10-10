package com.pushkar.codereview.analytics;

import com.pushkar.codereview.analytics.dto.AnalyticsFindingsResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsOverviewResponse;
import com.pushkar.codereview.analytics.dto.AnalyticsTrendItemResponse;
import com.pushkar.codereview.analytics.dto.RepositoryHealthStatus;
import com.pushkar.codereview.analytics.dto.RepositoryHealthSummaryResponse;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsControllerTest {

    private MockMvc mockMvc;
    private StubAnalyticsService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubAnalyticsService();
        AnalyticsController controller = new AnalyticsController(stubService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetOverview_Returns200OK() throws Exception {
        stubService.setOverviewResponse(new AnalyticsOverviewResponse(10, 8, 1, 1, 24));

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(10))
                .andExpect(jsonPath("$.completedReviews").value(8))
                .andExpect(jsonPath("$.failedReviews").value(1))
                .andExpect(jsonPath("$.inProgressReviews").value(1))
                .andExpect(jsonPath("$.totalFindings").value(24));
    }

    @Test
    void testGetFindings_Returns200OK() throws Exception {
        Map<String, Long> severities = Map.of("CRITICAL", 2L, "HIGH", 5L, "MEDIUM", 10L, "LOW", 5L, "INFO", 2L);
        Map<String, Long> categories = Map.of("SECURITY", 4L, "BUG", 8L, "CODE_STYLE", 6L, "PERFORMANCE", 3L, "MAINTAINABILITY", 2L, "OTHER", 1L);
        Map<String, Long> sources = Map.of("AI", 20L, "RULE", 4L);

        stubService.setFindingsResponse(new AnalyticsFindingsResponse(24, severities, categories, sources));

        mockMvc.perform(get("/api/v1/analytics/findings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").value(24))
                .andExpect(jsonPath("$.severityBreakdown.CRITICAL").value(2))
                .andExpect(jsonPath("$.severityBreakdown.HIGH").value(5))
                .andExpect(jsonPath("$.categoryBreakdown.SECURITY").value(4))
                .andExpect(jsonPath("$.sourceBreakdown.AI").value(20))
                .andExpect(jsonPath("$.sourceBreakdown.RULE").value(4));
    }

    @Test
    void testGetTrends_Returns200OK() throws Exception {
        AnalyticsTrendItemResponse day1 = new AnalyticsTrendItemResponse("2026-10-01", 3, 3, 0, 0, 8);
        AnalyticsTrendItemResponse day2 = new AnalyticsTrendItemResponse("2026-10-02", 0, 0, 0, 0, 0);

        stubService.setTrendsResponse(List.of(day1, day2));

        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$[0].totalReviews").value(3))
                .andExpect(jsonPath("$[0].totalFindings").value(8))
                .andExpect(jsonPath("$[1].date").value("2026-10-02"))
                .andExpect(jsonPath("$[1].totalReviews").value(0));
    }

    @Test
    void testGetRepositories_Returns200OK() throws Exception {
        RepositoryHealthSummaryResponse repo = new RepositoryHealthSummaryResponse(
                "octocat/hello-world", "octocat", "hello-world",
                5, 4, 1, 0, 12,
                1, 3, 5, 2, 1,
                Instant.parse("2026-10-08T12:00:00Z"),
                RepositoryHealthStatus.COMPLETED_WITH_FINDINGS
        );

        stubService.setRepositoriesResponse(List.of(repo));

        mockMvc.perform(get("/api/v1/analytics/repositories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository").value("octocat/hello-world"))
                .andExpect(jsonPath("$[0].totalReviews").value(5))
                .andExpect(jsonPath("$[0].totalFindings").value(12))
                .andExpect(jsonPath("$[0].healthStatus").value("COMPLETED_WITH_FINDINGS"))
                .andExpect(jsonPath("$[0].healthStatusDescription").value("Reviews completed with findings"));
    }

    @Test
    void testGetOverview_InvalidDateRange_Returns400BadRequest() throws Exception {
        stubService.setException(new IllegalArgumentException("Invalid date range: 'from' (2026-10-15) must not be after 'to' (2026-10-01)"));

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("from", "2026-10-15")
                        .param("to", "2026-10-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid date range: 'from' (2026-10-15) must not be after 'to' (2026-10-01)"));
    }

    @Test
    void testGetOverview_InvalidDateFormat_Returns400BadRequest() throws Exception {
        stubService.setException(new IllegalArgumentException("Invalid date format for 'from': invalid"));

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("from", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid date format for 'from': invalid"));
    }

    @Test
    void testGetOverview_AccessDenied_Returns403Forbidden() throws Exception {
        stubService.setException(new AccessDeniedException("User must be authenticated to access analytics"));

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // --- Stub Helper ---

    private static class StubAnalyticsService extends AnalyticsService {
        private AnalyticsOverviewResponse overviewResponse = new AnalyticsOverviewResponse();
        private AnalyticsFindingsResponse findingsResponse = new AnalyticsFindingsResponse();
        private List<AnalyticsTrendItemResponse> trendsResponse = List.of();
        private List<RepositoryHealthSummaryResponse> repositoriesResponse = List.of();
        private RuntimeException exception;

        public StubAnalyticsService() {
            super(null, null, null, null);
        }

        public void setOverviewResponse(AnalyticsOverviewResponse overviewResponse) {
            this.overviewResponse = overviewResponse;
        }

        public void setFindingsResponse(AnalyticsFindingsResponse findingsResponse) {
            this.findingsResponse = findingsResponse;
        }

        public void setTrendsResponse(List<AnalyticsTrendItemResponse> trendsResponse) {
            this.trendsResponse = trendsResponse;
        }

        public void setRepositoriesResponse(List<RepositoryHealthSummaryResponse> repositoriesResponse) {
            this.repositoriesResponse = repositoriesResponse;
        }

        public void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public AnalyticsOverviewResponse getOverview(String fromStr, String toStr, String repository, String owner) {
            if (exception != null) throw exception;
            return overviewResponse;
        }

        @Override
        public AnalyticsFindingsResponse getFindings(String fromStr, String toStr, String repository, String owner) {
            if (exception != null) throw exception;
            return findingsResponse;
        }

        @Override
        public List<AnalyticsTrendItemResponse> getTrends(String fromStr, String toStr, String repository, String owner) {
            if (exception != null) throw exception;
            return trendsResponse;
        }

        @Override
        public List<RepositoryHealthSummaryResponse> getRepositorySummaries(String fromStr, String toStr, String repository, String owner) {
            if (exception != null) throw exception;
            return repositoriesResponse;
        }
    }
}
