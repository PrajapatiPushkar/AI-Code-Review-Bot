package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.FindingSource;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewFindingMergerTest {

    private ReviewFindingMerger merger;

    @BeforeEach
    void setUp() {
        merger = new ReviewFindingMerger();
    }

    @Test
    void testMerge_TagsAiFindingsAndPreservesSummary() {
        ReviewFinding aiFinding = new ReviewFinding("src/App.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.SECURITY, "AI warning", "Fix SQLi");
        ReviewResult aiResult = new ReviewResult("AI generated summary", List.of(aiFinding));

        ReviewResult merged = merger.merge(aiResult, List.of());

        assertThat(merged.getSummary()).isEqualTo("AI generated summary");
        assertThat(merged.getFindings()).hasSize(1);
        ReviewFinding resultFinding = merged.getFindings().get(0);
        assertThat(resultFinding.getSource()).isEqualTo(FindingSource.AI);
        assertThat(resultFinding.getMessage()).isEqualTo("AI warning");
    }

    @Test
    void testMerge_AddsDeterministicRuleFindings() {
        RuleFinding ruleFinding = new RuleFinding(
                "RULE-JAVA-SYSTEM-OUT",
                "src/App.java",
                15,
                15,
                ReviewFindingSeverity.LOW,
                ReviewFindingCategory.CODE_STYLE,
                "Avoid System.out",
                "Use logger"
        );

        ReviewResult merged = merger.merge(null, List.of(ruleFinding));

        assertThat(merged.getFindings()).hasSize(1);
        ReviewFinding finding = merged.getFindings().get(0);
        assertThat(finding.getSource()).isEqualTo(FindingSource.RULE);
        assertThat(finding.getLine()).isEqualTo(15);
        assertThat(finding.getCategory()).isEqualTo(ReviewFindingCategory.CODE_STYLE);
        assertThat(finding.getSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
    }

    @Test
    void testMerge_Deduplication_PrefersDeterministicRule() {
        // AI found an issue at src/App.java:10 with category CODE_STYLE
        ReviewFinding aiFinding = new ReviewFinding(
                "src/App.java",
                10,
                ReviewFindingSeverity.MEDIUM,
                ReviewFindingCategory.CODE_STYLE,
                "AI: Print statement detected",
                "Remove print statement"
        );
        ReviewResult aiResult = new ReviewResult("Summary", List.of(aiFinding));

        // Deterministic rule detected the exact same location and category
        RuleFinding ruleFinding = new RuleFinding(
                "RULE-JAVA-SYSTEM-OUT",
                "src/App.java",
                10,
                10,
                ReviewFindingSeverity.LOW,
                ReviewFindingCategory.CODE_STYLE,
                "Deterministic: Avoid System.out",
                "Use logger"
        );

        ReviewResult merged = merger.merge(aiResult, List.of(ruleFinding));

        // Deduplication should collapse to 1 finding, preferring the deterministic rule
        assertThat(merged.getFindings()).hasSize(1);
        ReviewFinding finalFinding = merged.getFindings().get(0);
        assertThat(finalFinding.getSource()).isEqualTo(FindingSource.RULE);
        assertThat(finalFinding.getMessage()).isEqualTo("Deterministic: Avoid System.out");
        assertThat(finalFinding.getSeverity()).isEqualTo(ReviewFindingSeverity.LOW);
    }

    @Test
    void testMerge_DistinctLocations_BothPreserved() {
        ReviewFinding aiFinding = new ReviewFinding("src/App.java", 10, ReviewFindingSeverity.HIGH, ReviewFindingCategory.SECURITY, "AI warning", "Fix SQLi");
        RuleFinding ruleFinding = new RuleFinding(
                "RULE-JAVA-SYSTEM-OUT",
                "src/App.java",
                25,
                25,
                ReviewFindingSeverity.LOW,
                ReviewFindingCategory.CODE_STYLE,
                "Avoid System.out",
                "Use logger"
        );

        ReviewResult merged = merger.merge(new ReviewResult("AI Summary", List.of(aiFinding)), List.of(ruleFinding));

        assertThat(merged.getFindings()).hasSize(2);
        assertThat(merged.getFindings().get(0).getSource()).isEqualTo(FindingSource.AI);
        assertThat(merged.getFindings().get(1).getSource()).isEqualTo(FindingSource.RULE);
    }
}
