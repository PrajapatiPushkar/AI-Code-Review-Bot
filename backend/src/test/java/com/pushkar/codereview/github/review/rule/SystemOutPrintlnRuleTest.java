package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.rule.impl.SystemOutPrintlnRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemOutPrintlnRuleTest {

    private SystemOutPrintlnRule rule;

    @BeforeEach
    void setUp() {
        rule = new SystemOutPrintlnRule();
    }

    @Test
    void testMetadata() {
        assertThat(rule.getRuleId()).isEqualTo("RULE-JAVA-SYSTEM-OUT");
        assertThat(rule.getCategory()).isEqualTo(ReviewFindingCategory.CODE_STYLE);
        assertThat(rule.getSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
        assertThat(rule.getName()).isNotBlank();
        assertThat(rule.getDescription()).isNotBlank();
    }

    @Test
    void testPositiveMatch_SingleLine() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "package com.example;", false),
                new AnalyzedLine(2, "public class App {", false),
                new AnalyzedLine(3, "    public void run() {", false),
                new AnalyzedLine(4, "        System.out.println(\"Debugging message\");", true),
                new AnalyzedLine(5, "    }", false),
                new AnalyzedLine(6, "}", false)
        );
        AnalyzedFile file = new AnalyzedFile("src/main/java/App.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 10, "sha123", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
        RuleFinding finding = findings.get(0);
        assertThat(finding.getRuleId()).isEqualTo(SystemOutPrintlnRule.RULE_ID);
        assertThat(finding.getFilename()).isEqualTo("src/main/java/App.java");
        assertThat(finding.getLine()).isEqualTo(4);
        assertThat(finding.getEndLine()).isEqualTo(4);
        assertThat(finding.getSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
        assertThat(finding.getCategory()).isEqualTo(ReviewFindingCategory.CODE_STYLE);
        assertThat(finding.getMessage()).contains("System.out/err");
        assertThat(finding.getSuggestion()).contains("SLF4J");
    }

    @Test
    void testPositiveMatch_SystemErrAndPrintf() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(10, "System.err.println(\"Error occurred\");", true),
                new AnalyzedLine(11, "System.out.print(\"Inline\");", true),
                new AnalyzedLine(12, "System.out.printf(\"Count: %d\", count);", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/App.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 10, "sha123", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(3);
        assertThat(findings).extracting(RuleFinding::getLine).containsExactly(10, 11, 12);
    }

    @Test
    void testIgnoreUnchangedLines() {
        // Not marked as added (isAdded = false)
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(4, "System.out.println(\"Old code\");", false)
        );
        AnalyzedFile file = new AnalyzedFile("src/App.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 10, "sha123", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testIgnoreComments() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(4, "// System.out.println(\"commented\");", true),
                new AnalyzedLine(5, "* System.out.println(\"javadoc\");", true),
                new AnalyzedLine(6, "/* System.out.println(\"block comment\"); */", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/App.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 10, "sha123", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testIgnoreNonJavaFiles() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "System.out.println('Hello')", true)
        );
        AnalyzedFile file = new AnalyzedFile("script.py", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 10, "sha123", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testNullOrEmptyContext() {
        assertThat(rule.evaluate(null)).isEmpty();
        assertThat(rule.evaluate(new ReviewAnalysisContext("owner/repo", 1, "sha", null))).isEmpty();
        assertThat(rule.evaluate(new ReviewAnalysisContext("owner/repo", 1, "sha", List.of()))).isEmpty();
    }
}
