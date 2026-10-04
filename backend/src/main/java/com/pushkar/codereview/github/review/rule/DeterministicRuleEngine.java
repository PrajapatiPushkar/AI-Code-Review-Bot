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
        if (context == null || ruleRegistry == null) {
            return List.of();
        }

        List<RuleFinding> findings = new ArrayList<>();
        List<CodeQualityRule> rules = ruleRegistry.getRules();

        log.debug("Evaluating {} deterministic rules for repository={}, PR #{}",
                rules.size(), context.getRepository(), context.getPullRequestNumber());

        for (CodeQualityRule rule : rules) {
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
        List<RuleFinding> ruleFindings = evaluate(context);
        return ruleFindings.stream().map(RuleFinding::toReviewFinding).toList();
    }
}
