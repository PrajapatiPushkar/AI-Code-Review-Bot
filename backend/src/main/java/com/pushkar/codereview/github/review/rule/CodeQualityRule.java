package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;

import java.util.List;

/**
 * Interface representing an independent, deterministic code-quality rule.
 * Rules evaluate file changes in a pull request context and return zero or more findings.
 */
public interface CodeQualityRule {

    /**
     * Unique identifier for this rule (e.g. "RULE-JAVA-SYSTEM-OUT").
     */
    String getRuleId();

    /**
     * Human-readable name of the rule.
     */
    String getName();

    /**
     * Technical description explaining why the pattern is problematic and how to resolve it.
     */
    String getDescription();

    /**
     * The standard category under which findings from this rule fall.
     */
    ReviewFindingCategory getCategory();

    /**
     * The default severity assigned to findings produced by this rule.
     */
    ReviewFindingSeverity getSeverity();

    /**
     * Evaluates the rule against the provided review analysis context.
     *
     * @param context the context containing changed files and PR details
     * @return a list of detected findings (empty list if no violations found)
     */
    List<RuleFinding> evaluate(ReviewAnalysisContext context);
}
