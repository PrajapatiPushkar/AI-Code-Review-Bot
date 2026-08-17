package com.pushkar.codereview.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsTest {

    private MeterRegistry meterRegistry;
    private CodeReviewMetrics codeReviewMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        codeReviewMetrics = new CodeReviewMetrics(meterRegistry);
    }

    @Test
    void testBusinessMetrics_RegistrationAndIncrement() {
        codeReviewMetrics.recordSubmission(false);
        codeReviewMetrics.recordCompleted();
        codeReviewMetrics.recordExecutionTime(250);
        codeReviewMetrics.recordFindings(3);
        codeReviewMetrics.recordCommentsPosted(2);

        Counter submissionCounter = meterRegistry.find("code_review.submission.total").counter();
        assertThat(submissionCounter).isNotNull();
        assertThat(submissionCounter.count()).isEqualTo(1.0);

        Counter completedCounter = meterRegistry.find("code_review.completed.total").counter();
        assertThat(completedCounter).isNotNull();
        assertThat(completedCounter.count()).isEqualTo(1.0);

        Timer durationTimer = meterRegistry.find("code_review.duration").timer();
        assertThat(durationTimer).isNotNull();
        assertThat(durationTimer.count()).isEqualTo(1L);

        Counter findingsCounter = meterRegistry.find("code_review.findings.total").counter();
        assertThat(findingsCounter).isNotNull();
        assertThat(findingsCounter.count()).isEqualTo(3.0);

        Counter commentsPostedCounter = meterRegistry.find("code_review.comments.posted.total").counter();
        assertThat(commentsPostedCounter).isNotNull();
        assertThat(commentsPostedCounter.count()).isEqualTo(2.0);
    }

    @Test
    void testFailedMetric_IncrementsOnFailure() {
        codeReviewMetrics.recordFailed();

        Counter failedCounter = meterRegistry.find("code_review.failed.total").counter();
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(1.0);
    }

    @Test
    void testExternalDependencyMetrics_GitHubAndGemini() {
        codeReviewMetrics.recordExternalApiRequest("github", true, 120);
        codeReviewMetrics.recordExternalApiRequest("github", false, 50);
        codeReviewMetrics.recordExternalApiRequest("gemini", true, 800);

        Counter githubSuccess = meterRegistry.find("github.api.request").tag("status", "success").counter();
        assertThat(githubSuccess).isNotNull();
        assertThat(githubSuccess.count()).isEqualTo(1.0);

        Counter githubFailed = meterRegistry.find("github.api.request").tag("status", "failed").counter();
        assertThat(githubFailed).isNotNull();
        assertThat(githubFailed.count()).isEqualTo(1.0);

        Counter githubFailure = meterRegistry.find("github.api.failure").counter();
        assertThat(githubFailure).isNotNull();
        assertThat(githubFailure.count()).isEqualTo(1.0);

        Timer geminiDuration = meterRegistry.find("gemini.api.duration").timer();
        assertThat(geminiDuration).isNotNull();
        assertThat(geminiDuration.count()).isEqualTo(1L);
    }
}
