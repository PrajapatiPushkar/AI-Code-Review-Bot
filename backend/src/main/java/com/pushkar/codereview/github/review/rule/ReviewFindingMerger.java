package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.FindingSource;
import com.pushkar.codereview.github.review.dto.ReviewFinding;
import com.pushkar.codereview.github.review.dto.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges findings from deterministic rules and AI-based review into a unified, deduplicated ReviewResult.
 */
@Component
public class ReviewFindingMerger {

    private static final Logger log = LoggerFactory.getLogger(ReviewFindingMerger.class);

    /**
     * Merges AI findings with deterministic rule findings.
     *
     * Deduplication strategy:
     * Key = normalizedFilePath + ":" + line + ":" + category
     * When both a deterministic rule and the LLM flag the exact same file, line, and category,
     * the deterministic rule finding takes precedence due to guaranteed pattern accuracy.
     *
     * @param aiResult the review result produced by the AI review engine (nullable)
     * @param ruleFindings the list of findings produced by deterministic rules (nullable)
     * @return unified ReviewResult with merged findings
     */
    public ReviewResult merge(ReviewResult aiResult, List<RuleFinding> ruleFindings) {
        String baseSummary = (aiResult != null && aiResult.getSummary() != null)
                ? aiResult.getSummary()
                : "Code review analysis completed.";

        Map<String, ReviewFinding> mergedMap = new LinkedHashMap<>();

        // 1. Process AI findings first
        if (aiResult != null && aiResult.getFindings() != null) {
            for (ReviewFinding finding : aiResult.getFindings()) {
                if (finding != null) {
                    if (finding.getSource() == null) {
                        finding.setSource(FindingSource.AI);
                    }
                    String key = createKey(finding.getFilename(), finding.getLine(), finding.getCategory() != null ? finding.getCategory().name() : "");
                    mergedMap.put(key, finding);
                }
            }
        }

        // 2. Process and merge deterministic rule findings
        int rulesAdded = 0;
        int duplicatesResolved = 0;

        if (ruleFindings != null) {
            for (RuleFinding rf : ruleFindings) {
                if (rf != null) {
                    ReviewFinding converted = rf.toReviewFinding();
                    String key = createKey(converted.getFilename(), converted.getLine(), converted.getCategory() != null ? converted.getCategory().name() : "");

                    if (mergedMap.containsKey(key)) {
                        // Duplicate detected at same file, line, and category: prefer deterministic rule
                        duplicatesResolved++;
                        mergedMap.put(key, converted);
                    } else {
                        mergedMap.put(key, converted);
                        rulesAdded++;
                    }
                }
            }
        }

        List<ReviewFinding> unifiedFindings = new ArrayList<>(mergedMap.values());

        log.debug("Finding merge complete: total={}, ruleAdded={}, duplicatesResolved={}",
                unifiedFindings.size(), rulesAdded, duplicatesResolved);

        return new ReviewResult(baseSummary, unifiedFindings);
    }

    private String createKey(String filename, Integer line, String category) {
        String normFile = (filename != null) ? filename.trim().toLowerCase() : "";
        int normLine = (line != null) ? line : 0;
        String normCat = (category != null) ? category.trim().toUpperCase() : "";
        return normFile + ":" + normLine + ":" + normCat;
    }
}
