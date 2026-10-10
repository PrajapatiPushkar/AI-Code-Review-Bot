package com.pushkar.codereview.qualitygate;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.persistence.CodeReviewFinding;
import com.pushkar.codereview.github.review.persistence.CodeReviewStatus;
import com.pushkar.codereview.policy.RepositoryReviewPolicy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class QualityGateEvaluator {

    public QualityGateEvaluationResult evaluate(RepositoryReviewPolicy policy,
                                                CodeReviewStatus reviewStatus,
                                                List<CodeReviewFinding> findings) {
        Instant now = Instant.now();

        boolean enabled = policy == null || policy.isEnabled();
        ReviewFindingSeverity threshold = (policy != null && policy.getFailOnSeverity() != null)
                ? policy.getFailOnSeverity()
                : RepositoryReviewPolicy.DEFAULT_SEVERITY;

        // In-progress reviews must not be reported as PASS
        if (reviewStatus == CodeReviewStatus.IN_PROGRESS) {
            return new QualityGateEvaluationResult(
                    QualityGateStatus.NOT_EVALUATED,
                    enabled,
                    threshold,
                    0,
                    "Review is in progress; quality gate has not been evaluated yet.",
                    now
            );
        }

        // Failed reviews must not be reported as PASS
        if (reviewStatus == CodeReviewStatus.FAILED) {
            return new QualityGateEvaluationResult(
                    QualityGateStatus.NOT_EVALUATED,
                    enabled,
                    threshold,
                    0,
                    "Review execution failed; quality gate evaluation was aborted.",
                    now
            );
        }

        // Disabled quality gate
        if (!enabled) {
            return new QualityGateEvaluationResult(
                    QualityGateStatus.NOT_EVALUATED,
                    false,
                    threshold,
                    0,
                    "Quality gate evaluation is disabled for this repository.",
                    now
            );
        }

        // Evaluate completed review findings against threshold
        int failureCount = 0;
        if (findings != null) {
            for (CodeReviewFinding finding : findings) {
                if (finding != null && finding.getSeverity() != null) {
                    if (finding.getSeverity().ordinal() >= threshold.ordinal()) {
                        failureCount++;
                    }
                }
            }
        }

        if (failureCount > 0) {
            return new QualityGateEvaluationResult(
                    QualityGateStatus.FAIL,
                    true,
                    threshold,
                    failureCount,
                    String.format("Quality gate failed: %d finding(s) met or exceeded the %s severity threshold.", failureCount, threshold),
                    now
            );
        } else {
            return new QualityGateEvaluationResult(
                    QualityGateStatus.PASS,
                    true,
                    threshold,
                    0,
                    String.format("Quality gate passed: 0 findings met or exceeded the %s severity threshold.", threshold),
                    now
            );
        }
    }

    public record QualityGateEvaluationResult(
            QualityGateStatus status,
            boolean enabled,
            ReviewFindingSeverity failOnSeverity,
            int failureCount,
            String reason,
            Instant evaluatedAt
    ) {}
}
