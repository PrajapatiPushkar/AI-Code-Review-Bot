package com.pushkar.codereview.github.review.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry holding all enabled deterministic CodeQualityRule beans.
 * Automatically discovers rules registered as Spring beans.
 */
@Component
public class RuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);

    private final Map<String, CodeQualityRule> rulesById;

    @Autowired
    public RuleRegistry(List<CodeQualityRule> ruleList) {
        Map<String, CodeQualityRule> map = new LinkedHashMap<>();
        if (ruleList != null) {
            for (CodeQualityRule rule : ruleList) {
                if (rule != null && rule.getRuleId() != null) {
                    map.put(rule.getRuleId(), rule);
                }
            }
        }
        this.rulesById = Collections.unmodifiableMap(map);
        log.info("Initialized RuleRegistry with {} deterministic rules: {}", rulesById.size(), rulesById.keySet());
    }

    public List<CodeQualityRule> getRules() {
        return List.copyOf(rulesById.values());
    }

    public Optional<CodeQualityRule> getRule(String ruleId) {
        if (ruleId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rulesById.get(ruleId));
    }

    public int size() {
        return rulesById.size();
    }

    public java.util.Set<String> getRegisteredRuleIds() {
        return rulesById.keySet();
    }

    public boolean isSupportedRuleId(String ruleId) {
        return ruleId != null && rulesById.containsKey(ruleId);
    }
}
