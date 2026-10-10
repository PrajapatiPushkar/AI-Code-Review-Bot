package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service orchestrating the evaluation of all registered deterministic code-quality rules.
 */
@Service
public class DeterministicRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(DeterministicRuleEngine.class);

    private final RuleRegistry ruleRegistry;

    @Autowired
    public DeterministicRuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * Evaluates all enabled deterministic rules against the analysis context.
     *
     * @param context review analysis context containing changed files
     * @return list of deterministic rule findings
     */
    public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
        return evaluate(context, null);
    }

    /**
     * Evaluates registered deterministic rules that are in the enabledRuleIds set.
     * If enabledRuleIds is null, all registered rules are evaluated.
     * If enabledRuleIds is empty, no rules are evaluated and an empty list is returned.
     *
     * @param context review analysis context containing changed files
     * @param enabledRuleIds set of rule IDs that are enabled, or null to evaluate all registered rules
     * @return list of deterministic rule findings
     */
    public List<RuleFinding> evaluate(ReviewAnalysisContext context, java.util.Set<String> enabledRuleIds) {
        if (context == null || ruleRegistry == null) {
            return List.of();
        }

        List<RuleFinding> findings = new ArrayList<>();
        List<CodeQualityRule> rules = ruleRegistry.getRules();

        log.debug("Evaluating deterministic rules for repository={}, PR #{} with enabledRuleIds={}",
                context.getRepository(), context.getPullRequestNumber(), enabledRuleIds);

        for (CodeQualityRule rule : rules) {
            if (enabledRuleIds != null && !enabledRuleIds.contains(rule.getRuleId())) {
                log.debug("Skipping disabled rule '{}' for repository={}", rule.getRuleId(), context.getRepository());
                continue;
            }
            try {
                List<RuleFinding> ruleResults = rule.evaluate(context);
                if (ruleResults != null && !ruleResults.isEmpty()) {
                    findings.addAll(ruleResults);
                }
            } catch (Exception ex) {
                log.warn("Rule '{}' failed during evaluation for repository={}, PR #{}: {}",
                        rule.getRuleId(), context.getRepository(), context.getPullRequestNumber(), ex.getMessage(), ex);
            }
        }

        log.info("Deterministic rule engine evaluation completed: {} findings from {} rules",
                findings.size(), rules.size());

        return findings;
    }

    /**
     * Evaluates rules and directly converts results to ReviewFinding domain objects.
     */
    public List<ReviewFinding> evaluateToReviewFindings(ReviewAnalysisContext context) {
        return evaluateToReviewFindings(context, null);
    }

    /**
     * Evaluates enabled rules and converts results to ReviewFinding domain objects.
     */
    public List<ReviewFinding> evaluateToReviewFindings(ReviewAnalysisContext context, java.util.Set<String> enabledRuleIds) {
        List<RuleFinding> ruleFindings = evaluate(context, enabledRuleIds);
        return ruleFindings.stream().map(RuleFinding::toReviewFinding).toList();
    }
}
