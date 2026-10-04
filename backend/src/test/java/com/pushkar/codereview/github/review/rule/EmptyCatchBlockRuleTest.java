package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.rule.impl.EmptyCatchBlockRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmptyCatchBlockRuleTest {

    private EmptyCatchBlockRule rule;

    @BeforeEach
    void setUp() {
        rule = new EmptyCatchBlockRule();
    }

    @Test
    void testMetadata() {
        assertThat(rule.getRuleId()).isEqualTo("RULE-JAVA-EMPTY-CATCH");
        assertThat(rule.getCategory()).isEqualTo(ReviewFindingCategory.BUG);
        assertThat(rule.getSeverity()).isEqualTo(ReviewFindingSeverity.MEDIUM);
        assertThat(rule.getName()).isNotBlank();
        assertThat(rule.getDescription()).isNotBlank();
    }

    @Test
    void testSingleLineEmptyCatch() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(10, "try {", false),
                new AnalyzedLine(11, "    doSomething();", false),
                new AnalyzedLine(12, "} catch (Exception e) {}", true),
                new AnalyzedLine(13, "return;", false)
        );
        AnalyzedFile file = new AnalyzedFile("src/Service.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
        RuleFinding finding = findings.get(0);
        assertThat(finding.getRuleId()).isEqualTo(EmptyCatchBlockRule.RULE_ID);
        assertThat(finding.getFilename()).isEqualTo("src/Service.java");
        assertThat(finding.getLine()).isEqualTo(12);
        assertThat(finding.getEndLine()).isEqualTo(12);
        assertThat(finding.getSeverity()).isEqualTo(ReviewFindingSeverity.MEDIUM);
        assertThat(finding.getCategory()).isEqualTo(ReviewFindingCategory.BUG);
    }

    @Test
    void testMultiLineEmptyCatch() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(20, "try {", false),
                new AnalyzedLine(21, "    riskyOperation();", false),
                new AnalyzedLine(22, "} catch (IOException ex) {", true),
                new AnalyzedLine(23, "", true),
                new AnalyzedLine(24, "}", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/Service.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
        RuleFinding finding = findings.get(0);
        assertThat(finding.getLine()).isEqualTo(22);
        assertThat(finding.getEndLine()).isEqualTo(24);
        assertThat(finding.getSeverity()).isEqualTo(ReviewFindingSeverity.MEDIUM);
        assertThat(finding.getCategory()).isEqualTo(ReviewFindingCategory.BUG);
    }

    @Test
    void testMultiLineCatchWithOnlyComments() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(30, "catch (IllegalArgumentException e) {", true),
                new AnalyzedLine(31, "    // ignore this exception for now", true),
                new AnalyzedLine(32, "    /* nothing to do */", true),
                new AnalyzedLine(33, "}", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/Service.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getLine()).isEqualTo(30);
        assertThat(findings.get(0).getEndLine()).isEqualTo(33);
    }

    @Test
    void testHandledCatch_DoesNotMatch() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(40, "catch (Exception e) {", true),
                new AnalyzedLine(41, "    log.error(\"Failed operation\", e);", true),
                new AnalyzedLine(42, "    throw new CustomException(e);", true),
                new AnalyzedLine(43, "}", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/Service.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testIgnoreUnchangedLines() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(50, "} catch (Exception e) {}", false)
        );
        AnalyzedFile file = new AnalyzedFile("src/Service.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testNonJavaFile_Ignored() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "catch (e) {}", true)
        );
        AnalyzedFile file = new AnalyzedFile("script.js", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }
}
