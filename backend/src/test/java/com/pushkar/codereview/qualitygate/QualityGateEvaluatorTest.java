package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReview;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.policy.RepositoryReviewPolicy;
import com.pushkar.codereview.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QualityGateEvaluatorTest {

    private QualityGateEvaluator evaluator;
    private Repository repository;
    private RepositoryReviewPolicy policy;
    private CodeReview review;

    @BeforeEach
    void setUp() {
        evaluator = new QualityGateEvaluator();
        repository = new Repository();
        repository.setId(1L);
        repository.setName("demo-repo");
        repository.setFullName("octocat/demo-repo");

        policy = RepositoryReviewPolicy.createDefault(repository);
        policy.setEnabled(true);
        policy.setFailOnSeverity(ReviewFindingSeverity.HIGH);

        review = new CodeReview(123456L, "octocat", "demo-repo", 42);
        review.setId(10L);
    }

    @Test
    void testEvaluate_CompletedReviewWithNoFindings_ReturnsPass() {
        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.COMPLETED,
                List.of()
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.PASS);
        assertThat(result.enabled()).isTrue();
        assertThat(result.failOnSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.reason()).contains("Quality gate passed: 0 findings met or exceeded the HIGH severity threshold.");
    }

    @Test
    void testEvaluate_CompletedReviewWithFindingExactlyAtThreshold_ReturnsFail() {
        CodeReviewFinding highFinding = new CodeReviewFinding(
                review, "App.java", 10, 15,
                ReviewFindingSeverity.HIGH, ReviewFindingCategory.SECURITY,
                "SQL injection vulnerability", "Use parameterized query"
        );

        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.COMPLETED,
                List.of(highFinding)
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.FAIL);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.reason()).contains("Quality gate failed: 1 finding(s) met or exceeded the HIGH severity threshold.");
    }

    @Test
    void testEvaluate_CompletedReviewWithFindingAboveThreshold_ReturnsFail() {
        policy.setFailOnSeverity(ReviewFindingSeverity.HIGH);

        CodeReviewFinding criticalFinding = new CodeReviewFinding(
                review, "Auth.java", 5, 8,
                ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY,
                "Hardcoded secret", "Remove secret"
        );

        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.COMPLETED,
                List.of(criticalFinding)
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.FAIL);
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    void testEvaluate_CompletedReviewWithFindingsBelowThreshold_ReturnsPass() {
        policy.setFailOnSeverity(ReviewFindingSeverity.HIGH);

        CodeReviewFinding lowFinding = new CodeReviewFinding(
                review, "Utils.java", 12, 12,
                ReviewFindingSeverity.LOW, ReviewFindingCategory.CODE_STYLE,
                "Avoid println", "Use logger"
        );
        CodeReviewFinding mediumFinding = new CodeReviewFinding(
                review, "Service.java", 20, 25,
                ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.PERFORMANCE,
                "Inefficient loop", "Use Map"
        );

        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.COMPLETED,
                List.of(lowFinding, mediumFinding)
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.PASS);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.reason()).contains("Quality gate passed");
    }

    @Test
    void testEvaluate_DisabledGate_ReturnsNotEvaluated() {
        policy.setEnabled(false);

        CodeReviewFinding criticalFinding = new CodeReviewFinding(
                review, "Auth.java", 5, 8,
                ReviewFindingSeverity.CRITICAL, ReviewFindingCategory.SECURITY,
                "Critical bug", "Fix"
        );

        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.COMPLETED,
                List.of(criticalFinding)
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.NOT_EVALUATED);
        assertThat(result.enabled()).isFalse();
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.reason()).contains("disabled for this repository");
    }

    @Test
    void testEvaluate_InProgressReview_ReturnsNotEvaluated() {
        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.IN_PROGRESS,
                List.of()
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.NOT_EVALUATED);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.reason()).contains("in progress");
    }

    @Test
    void testEvaluate_FailedReview_ReturnsNotEvaluated() {
        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                policy,
                CodeReviewStatus.FAILED,
                List.of()
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.NOT_EVALUATED);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.reason()).contains("Review execution failed");
    }

    @Test
    void testEvaluate_NullPolicy_FallsBackToDefaultThresholdAndEvaluates() {
        QualityGateEvaluator.QualityGateEvaluationResult result = evaluator.evaluate(
                null,
                CodeReviewStatus.COMPLETED,
                List.of()
        );

        assertThat(result.status()).isEqualTo(QualityGateStatus.PASS);
        assertThat(result.failOnSeverity()).isEqualTo(ReviewFindingSeverity.HIGH);
    }
}
