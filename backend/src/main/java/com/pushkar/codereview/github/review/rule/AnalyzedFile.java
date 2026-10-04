package com.pushkar.codereview.github.review.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a file subject to deterministic code-quality rule evaluation.
 * Holds line-accurate representations parsed from pull request git diffs or raw source code.
 */
public class AnalyzedFile {

    private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("^@@\\s+-\\d+(?:,\\d+)?\\s+\\+(\\d+)(?:,\\d+)?\\s+@@.*");

    private final String filename;
    private final String status;
    private final String patch;
    private final List<AnalyzedLine> lines;

    public AnalyzedFile(String filename, String status, String patch, List<AnalyzedLine> lines) {
        this.filename = filename != null ? filename : "";
        this.status = status != null ? status : "modified";
        this.patch = patch;
        this.lines = lines != null ? Collections.unmodifiableList(lines) : Collections.emptyList();
    }

    public AnalyzedFile(String filename, List<AnalyzedLine> lines) {
        this(filename, "modified", null, lines);
    }

    /**
     * Parses a git unified diff patch to accurately reconstruct line numbers for added and context lines.
     */
    public static AnalyzedFile fromPatch(String filename, String status, String patch) {
        if (patch == null || patch.isBlank()) {
            return new AnalyzedFile(filename, status, patch, Collections.emptyList());
        }

        List<AnalyzedLine> lines = new ArrayList<>();
        String[] patchLines = patch.split("\\r?\\n");
        int currentNewLine = -1;

        for (String patchLine : patchLines) {
            Matcher matcher = HUNK_HEADER_PATTERN.matcher(patchLine);
            if (matcher.matches()) {
                currentNewLine = Integer.parseInt(matcher.group(1));
                continue;
            }

            if (currentNewLine < 0) {
                // Pre-hunk metadata
                continue;
            }

            if (patchLine.startsWith("+") && !patchLine.startsWith("+++")) {
                // Line added or modified in the target revision
                lines.add(new AnalyzedLine(currentNewLine, patchLine.substring(1), true));
                currentNewLine++;
            } else if (patchLine.startsWith("-") && !patchLine.startsWith("---")) {
                // Deleted line from previous revision (not present in new revision)
                // Does not advance currentNewLine
            } else if (patchLine.startsWith(" ")) {
                // Context line in both revisions
                lines.add(new AnalyzedLine(currentNewLine, patchLine.substring(1), false));
                currentNewLine++;
            } else if (patchLine.startsWith("\\")) {
                // Diff notice e.g. "\ No newline at end of file" - ignore
            }
        }

        return new AnalyzedFile(filename, status, patch, lines);
    }

    /**
     * Constructs an AnalyzedFile from raw source code where every line is considered active.
     */
    public static AnalyzedFile fromSource(String filename, String sourceCode) {
        if (sourceCode == null) {
            return new AnalyzedFile(filename, "added", null, Collections.emptyList());
        }

        List<AnalyzedLine> lines = new ArrayList<>();
        String[] rawLines = sourceCode.split("\\r?\\n", -1);
        for (int i = 0; i < rawLines.length; i++) {
            lines.add(new AnalyzedLine(i + 1, rawLines[i], true));
        }

        return new AnalyzedFile(filename, "added", null, lines);
    }

    public String getFilename() {
        return filename;
    }

    public String getStatus() {
        return status;
    }

    public String getPatch() {
        return patch;
    }

    public List<AnalyzedLine> getLines() {
        return lines;
    }

    public List<AnalyzedLine> getAddedLines() {
        return lines.stream().filter(AnalyzedLine::isAdded).toList();
    }

    public boolean isJavaFile() {
        return filename != null && filename.endsWith(".java");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyzedFile that = (AnalyzedFile) o;
        return Objects.equals(filename, that.filename) &&
                Objects.equals(status, that.status) &&
                Objects.equals(lines, that.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, status, lines);
    }

    @Override
    public String toString() {
        return "AnalyzedFile{" +
                "filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", lineCount=" + lines.size() +
                '}';
    }
}
