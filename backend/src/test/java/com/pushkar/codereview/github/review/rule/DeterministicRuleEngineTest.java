package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicRuleEngineTest {

    @Test
    void testEvaluate_AggregatesFindingsFromMultipleRules() {
        CodeQualityRule rule1 = new CodeQualityRule() {
            @Override public String getRuleId() { return "RULE-1"; }
            @Override public String getName() { return "Rule 1"; }
            @Override public String getDescription() { return "Rule 1 desc"; }
            @Override public ReviewFindingCategory getCategory() { return ReviewFindingCategory.CODE_STYLE; }
            @Override public ReviewFindingSeverity getSeverity() { return ReviewFindingSeverity.LOW; }
            @Override public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
                return List.of(new RuleFinding("RULE-1", "App.java", 1, 1, ReviewFindingSeverity.LOW, ReviewFindingCategory.CODE_STYLE, "Rule 1 hit", "Fix 1"));
            }
        };

        CodeQualityRule rule2 = new CodeQualityRule() {
            @Override public String getRuleId() { return "RULE-2"; }
            @Override public String getName() { return "Rule 2"; }
            @Override public String getDescription() { return "Rule 2 desc"; }
            @Override public ReviewFindingCategory getCategory() { return ReviewFindingCategory.BUG; }
            @Override public ReviewFindingSeverity getSeverity() { return ReviewFindingSeverity.MEDIUM; }
            @Override public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
                return List.of(new RuleFinding("RULE-2", "App.java", 10, 12, ReviewFindingSeverity.MEDIUM, ReviewFindingCategory.BUG, "Rule 2 hit", "Fix 2"));
            }
        };

        RuleRegistry registry = new RuleRegistry(List.of(rule1, rule2));
        DeterministicRuleEngine engine = new DeterministicRuleEngine(registry);

        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of());
        List<RuleFinding> findings = engine.evaluate(context);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getRuleId()).isEqualTo("RULE-1");
        assertThat(findings.get(1).getRuleId()).isEqualTo("RULE-2");

        List<ReviewFinding> reviewFindings = engine.evaluateToReviewFindings(context);
        assertThat(reviewFindings).hasSize(2);
        assertThat(reviewFindings.get(0).getMessage()).isEqualTo("Rule 1 hit");
        assertThat(reviewFindings.get(1).getMessage()).isEqualTo("Rule 2 hit");
    }

    @Test
    void testEvaluate_FaultTolerance_RuleExceptionDoesNotHaltOtherRules() {
        CodeQualityRule failingRule = new CodeQualityRule() {
            @Override public String getRuleId() { return "RULE-FAIL"; }
            @Override public String getName() { return "Failing Rule"; }
            @Override public String getDescription() { return "Throws"; }
            @Override public ReviewFindingCategory getCategory() { return ReviewFindingCategory.BUG; }
            @Override public ReviewFindingSeverity getSeverity() { return ReviewFindingSeverity.HIGH; }
            @Override public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
                throw new RuntimeException("Simulated unexpected exception");
            }
        };

        CodeQualityRule workingRule = new CodeQualityRule() {
            @Override public String getRuleId() { return "RULE-OK"; }
            @Override public String getName() { return "Working Rule"; }
            @Override public String getDescription() { return "Works"; }
            @Override public ReviewFindingCategory getCategory() { return ReviewFindingCategory.MAINTAINABILITY; }
            @Override public ReviewFindingSeverity getSeverity() { return ReviewFindingSeverity.INFO; }
            @Override public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
                return List.of(new RuleFinding("RULE-OK", "App.java", 5, 5, ReviewFindingSeverity.INFO, ReviewFindingCategory.MAINTAINABILITY, "Works fine", null));
            }
        };

        RuleRegistry registry = new RuleRegistry(List.of(failingRule, workingRule));
        DeterministicRuleEngine engine = new DeterministicRuleEngine(registry);

        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of());
        List<RuleFinding> findings = engine.evaluate(context);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo("RULE-OK");
    }

    @Test
    void testEvaluate_NullContextReturnsEmpty() {
        RuleRegistry registry = new RuleRegistry(List.of());
        DeterministicRuleEngine engine = new DeterministicRuleEngine(registry);

        assertThat(engine.evaluate(null)).isEmpty();
    }
}
