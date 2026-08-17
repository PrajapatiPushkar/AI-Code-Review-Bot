package com.pushkar.codereview.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CodeReviewMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private CodeReviewMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new CodeReviewMetrics(meterRegistry);
    }

    @Test
    void testRecordSubmission_IncrementsNewAndDuplicateCounters() {
        metrics.recordSubmission(false);
        metrics.recordSubmission(false);
        metrics.recordSubmission(true);

        assertThat(metrics.getSubmissionsCounter().count()).isEqualTo(2.0);
        assertThat(metrics.getDuplicateSubmissionsCounter().count()).isEqualTo(1.0);
    }

    @Test
    void testRecordCompletedAndFailed_IncrementsCounters() {
        metrics.recordCompleted();
        metrics.recordCompleted();
        metrics.recordFailed();

        assertThat(metrics.getCompletedCounter().count()).isEqualTo(2.0);
        assertThat(metrics.getFailedCounter().count()).isEqualTo(1.0);
    }

    @Test
    void testRecordFindingsAndComments_IncrementsCounters() {
        metrics.recordFindings(5);
        metrics.recordFindings(3);
        metrics.recordCommentsPosted(4);

        assertThat(metrics.getFindingsCounter().count()).isEqualTo(8.0);
        assertThat(metrics.getCommentsCounter().count()).isEqualTo(4.0);
    }

    @Test
    void testRecordExecutionTime_UpdatesTimer() {
        metrics.recordExecutionTime(1500L);

        assertThat(metrics.getExecutionTimer().count()).isEqualTo(1L);
        assertThat(metrics.getExecutionTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(1500.0);
    }

    @Test
    void testRecordAiExecutionTime_UpdatesTimer() {
        metrics.recordAiExecutionTime(800L);

        assertThat(metrics.getAiExecutionTimer().count()).isEqualTo(1L);
        assertThat(metrics.getAiExecutionTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(800.0);
    }

    @Test
    void testRecordGithubFailure_IncrementsCounter() {
        metrics.recordGithubFailure();
        metrics.recordGithubFailure();

        assertThat(metrics.getGithubFailuresCounter().count()).isEqualTo(2.0);
    }

    @Test
    void testInProgressGauge_IncrementsAndDecrements() {
        assertThat(metrics.getInProgressCount()).isEqualTo(0);

        metrics.incrementInProgress();
        metrics.incrementInProgress();
        assertThat(metrics.getInProgressCount()).isEqualTo(2);

        metrics.decrementInProgress();
        assertThat(metrics.getInProgressCount()).isEqualTo(1);
    }
}
