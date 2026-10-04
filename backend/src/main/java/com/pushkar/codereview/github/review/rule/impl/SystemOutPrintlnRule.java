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
import java.util.regex.Pattern;

/**
 * Deterministic rule to detect raw System.out.println / System.err.print calls in Java files.
 */
@Component
public class SystemOutPrintlnRule implements CodeQualityRule {

    public static final String RULE_ID = "RULE-JAVA-SYSTEM-OUT";

    private static final Pattern SYSTEM_OUT_PATTERN = Pattern.compile("System\\.(out|err)\\.(println|print|printf)\\s*\\(");

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Avoid System.out/err in Production Code";
    }

    @Override
    public String getDescription() {
        return "Standard output stream printing (System.out.println, System.err.print, etc.) bypasses application logging configurations, cannot be routed or filtered by log level, and can degrade performance.";
    }

    @Override
    public ReviewFindingCategory getCategory() {
        return ReviewFindingCategory.CODE_STYLE;
    }

    @Override
    public ReviewFindingSeverity getSeverity() {
        return ReviewFindingSeverity.LOW;
    }

    @Override
    public List<RuleFinding> evaluate(ReviewAnalysisContext context) {
        if (context == null || context.getFiles() == null) {
            return List.of();
        }

        List<RuleFinding> findings = new ArrayList<>();

        for (AnalyzedFile file : context.getFiles()) {
            if (!file.isJavaFile()) {
                continue;
            }

            for (AnalyzedLine line : file.getLines()) {
                if (!line.isAdded()) {
                    continue;
                }

                String trimmed = line.getContent().trim();
                // Skip comment lines
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                    continue;
                }

                if (SYSTEM_OUT_PATTERN.matcher(trimmed).find()) {
                    findings.add(new RuleFinding(
                            RULE_ID,
                            file.getFilename(),
                            line.getLineNumber(),
                            line.getLineNumber(),
                            getSeverity(),
                            getCategory(),
                            "Avoid direct console output using System.out/err in production code.",
                            "Use a configured SLF4J logger (e.g. log.info(...) or log.debug(...)) instead of System.out/err."
                    ));
                }
            }
        }

        return findings;
    }
}
