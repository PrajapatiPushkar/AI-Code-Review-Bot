package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.rule.impl.TodoCommentRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TodoCommentRuleTest {

    private TodoCommentRule rule;

    @BeforeEach
    void setUp() {
        rule = new TodoCommentRule();
    }

    @Test
    void testMetadata() {
        assertThat(rule.getRuleId()).isEqualTo("RULE-TODO-FIXME");
        assertThat(rule.getCategory()).isEqualTo(ReviewFindingCategory.MAINTAINABILITY);
        assertThat(rule.getSeverity()).isEqualTo(ReviewFindingSeverity.INFO);
        assertThat(rule.getName()).isNotBlank();
        assertThat(rule.getDescription()).isNotBlank();
    }

    @Test
    void testMatchesTodoAndFixme() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "// TODO: refactor this method later", true),
                new AnalyzedLine(2, "// FIXME: handle edge cases when null", true),
                new AnalyzedLine(3, "/* TODO implement caching */", true),
                new AnalyzedLine(4, "# TODO: python comment style", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/main/java/Processor.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(4);
        assertThat(findings).extracting(RuleFinding::getLine).containsExactly(1, 2, 3, 4);
        assertThat(findings.get(0).getSeverity()).isEqualTo(ReviewFindingSeverity.INFO);
        assertThat(findings.get(0).getCategory()).isEqualTo(ReviewFindingCategory.MAINTAINABILITY);
    }

    @Test
    void testIgnoreNormalCommentsAndCode() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "// Regular comment explaining logic", true),
                new AnalyzedLine(2, "String todoList = \"todos\";", true),
                new AnalyzedLine(3, "int total = 100;", true)
        );
        AnalyzedFile file = new AnalyzedFile("src/main/java/Processor.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testIgnoreUnchangedLines() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "// TODO: legacy todo", false)
        );
        AnalyzedFile file = new AnalyzedFile("src/main/java/Processor.java", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(file));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }

    @Test
    void testSupportedExtensions() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "// TODO: in ts file", true)
        );
        AnalyzedFile tsFile = new AnalyzedFile("frontend/src/App.tsx", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(tsFile));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
    }

    @Test
    void testUnsupportedExtensionIgnored() {
        List<AnalyzedLine> lines = List.of(
                new AnalyzedLine(1, "// TODO: in markdown doc", true)
        );
        AnalyzedFile mdFile = new AnalyzedFile("README.md", lines);
        ReviewAnalysisContext context = new ReviewAnalysisContext("owner/repo", 1, "sha", List.of(mdFile));

        List<RuleFinding> findings = rule.evaluate(context);

        assertThat(findings).isEmpty();
    }
}
