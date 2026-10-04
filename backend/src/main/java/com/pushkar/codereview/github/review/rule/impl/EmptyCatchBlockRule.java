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
 * Deterministic rule to detect empty catch blocks that silently swallow exceptions.
 */
@Component
public class EmptyCatchBlockRule implements CodeQualityRule {

    public static final String RULE_ID = "RULE-JAVA-EMPTY-CATCH";

    // Matches single-line catch with empty braces, e.g. catch (Exception e) {} or } catch (Exception e) { }
    private static final Pattern SINGLE_LINE_EMPTY_CATCH = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");

    // Matches the start of a catch block, e.g. catch (Exception e) {
    private static final Pattern CATCH_HEADER_PATTERN = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{");

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Avoid Empty Catch Blocks";
    }

    @Override
    public String getDescription() {
        return "Empty catch blocks silently swallow exceptions without handling or logging, obscuring bugs and making troubleshooting difficult.";
    }

    @Override
    public ReviewFindingCategory getCategory() {
        return ReviewFindingCategory.BUG;
    }

    @Override
    public ReviewFindingSeverity getSeverity() {
        return ReviewFindingSeverity.MEDIUM;
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

            List<AnalyzedLine> lines = file.getLines();
            int size = lines.size();

            for (int i = 0; i < size; i++) {
                AnalyzedLine line = lines.get(i);
                String trimmed = line.getContent().trim();

                // Skip non-code lines
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                    continue;
                }

                // Check 1: Single-line empty catch
                if (SINGLE_LINE_EMPTY_CATCH.matcher(trimmed).find()) {
                    if (line.isAdded()) {
                        findings.add(new RuleFinding(
                                RULE_ID,
                                file.getFilename(),
                                line.getLineNumber(),
                                line.getLineNumber(),
                                getSeverity(),
                                getCategory(),
                                "Empty catch block detected. Swallowing exceptions without logging or rethrowing hides critical failures.",
                                "Log the caught exception using a logger (e.g. log.error(\"Operation failed\", e)), rethrow it wrapped, or document why it is safely ignored."
                        ));
                    }
                    continue;
                }

                // Check 2: Multi-line catch block
                if (CATCH_HEADER_PATTERN.matcher(trimmed).find()) {
                    // Check if any statement exists between this catch header and its closing brace
                    boolean hasCodeInside = false;
                    boolean foundCloseBrace = false;
                    int endLineNumber = line.getLineNumber();
                    boolean anyLineAdded = line.isAdded();

                    for (int j = i + 1; j < size; j++) {
                        AnalyzedLine nextLine = lines.get(j);
                        if (nextLine.isAdded()) {
                            anyLineAdded = true;
                        }

                        String nextTrimmed = nextLine.getContent().trim();

                        if (nextTrimmed.equals("}") || nextTrimmed.startsWith("}")) {
                            foundCloseBrace = true;
                            endLineNumber = nextLine.getLineNumber();
                            break;
                        }

                        // Ignore empty lines and comment lines
                        if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("//") || nextTrimmed.startsWith("*") || nextTrimmed.startsWith("/*")) {
                            continue;
                        }

                        // Any executable code statement
                        hasCodeInside = true;
                        break;
                    }

                    if (foundCloseBrace && !hasCodeInside && anyLineAdded) {
                        findings.add(new RuleFinding(
                                RULE_ID,
                                file.getFilename(),
                                line.getLineNumber(),
                                endLineNumber,
                                getSeverity(),
                                getCategory(),
                                "Empty catch block detected. Swallowing exceptions without logging or rethrowing hides critical failures.",
                                "Log the caught exception using a logger (e.g. log.error(\"Operation failed\", e)), rethrow it wrapped, or document why it is safely ignored."
                        ));
                        // Fast forward to after the closing brace
                        while (i < size && lines.get(i).getLineNumber() < endLineNumber) {
                            i++;
                        }
                    }
                }
            }
        }

        return findings;
    }
}
