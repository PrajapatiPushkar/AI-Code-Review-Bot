package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.rule.impl.EmptyCatchBlockRule;
import com.pushkar.codereview.github.review.rule.impl.SystemOutPrintlnRule;
import com.pushkar.codereview.github.review.rule.impl.TodoCommentRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleRegistryTest {

    @Test
    void testRegistryInitializationAndRetrieval() {
        SystemOutPrintlnRule rule1 = new SystemOutPrintlnRule();
        EmptyCatchBlockRule rule2 = new EmptyCatchBlockRule();
        TodoCommentRule rule3 = new TodoCommentRule();

        RuleRegistry registry = new RuleRegistry(List.of(rule1, rule2, rule3));

        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.getRules()).containsExactly(rule1, rule2, rule3);

        assertThat(registry.getRule("RULE-JAVA-SYSTEM-OUT")).contains(rule1);
        assertThat(registry.getRule("RULE-JAVA-EMPTY-CATCH")).contains(rule2);
        assertThat(registry.getRule("RULE-TODO-FIXME")).contains(rule3);
        assertThat(registry.getRule("NON_EXISTENT")).isEmpty();
        assertThat(registry.getRule(null)).isEmpty();
    }

    @Test
    void testRegistryWithNullList() {
        RuleRegistry registry = new RuleRegistry(null);
        assertThat(registry.size()).isEqualTo(0);
        assertThat(registry.getRules()).isEmpty();
    }
}
