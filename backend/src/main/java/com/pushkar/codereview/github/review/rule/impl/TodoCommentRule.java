package com.pushkar.codereview.github.review.rule.impl;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.github.review.rule.AnalyzedFile;
import com.pushkar.codereview.github.review.rule.AnalyzedLine;
import com.pushkar.codereview.github.review.rule.CodeQualityRule;
import com.pushkar.codereview.github.review.rule.ReviewAnalysisContext;
import com.pushkar.codereview.github.review.rule.RuleFinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic rule to detect unresolved TODO and FIXME comment markers in source code changes.
 */
@Component
public class TodoCommentRule implements CodeQualityRule {

    public static final String RULE_ID = "RULE-TODO-FIXME";

    // Matches comments with TODO or FIXME (e.g. // TODO, /* FIXME, * TODO, # TODO)
    private static final Pattern TODO_PATTERN = Pattern.compile("(?i)(?://|/\\*|\\*|#)\\s*(?:TODO|FIXME)\\b");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".kt", ".groovy", ".scala",
            ".js", ".jsx", ".ts", ".tsx",
            ".py", ".go", ".rb", ".c", ".cpp", ".h", ".cs", ".sql"
    );

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Unresolved TODO or FIXME Marker";
    }

    @Override
    public String getDescription() {
        return "TODO and FIXME comments represent incomplete implementation, temporary workarounds, or deferred technical debt that should be addressed before merging.";
    }

    @Override
    public ReviewFindingCategory getCategory() {
        return ReviewFindingCategory.MAINTAINABILITY;
    }

    @Override
    public ReviewFindingSeverity getSeverity() {
        return ReviewFindingSeverity.INFO;
    }

    @Override
    public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
        if (context == null || context.getFiles() == null) {
            return List.of();
        }

        List<RuleFinding> findings = new ArrayList<>();

        for (AnalyzedFile file : context.getFiles()) {
            if (!isSupportedFile(file.getFilename())) {
                continue;
            }

            for (AnalyzedLine line : file.getLines()) {
                if (!line.isAdded()) {
                    continue;
                }

                String trimmed = line.getContent().trim();
                if (TODO_PATTERN.matcher(trimmed).find()) {
                    findings.add(new RuleFinding(
                            RULE_ID,
                            file.getFilename(),
                            line.getLineNumber(),
                            line.getLineNumber(),
                            getSeverity(),
                            getCategory(),
                            "Unresolved TODO/FIXME marker found in code change.",
                            "Resolve this incomplete implementation, remove the marker, or track it in an issue tracker before merging."
                    ));
                }
            }
        }

        return findings;
    }

    private boolean isSupportedFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
