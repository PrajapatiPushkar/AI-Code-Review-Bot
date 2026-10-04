package com.pushkar.codereview.github.review.rule;

import java.util.Objects;

/**
 * Represents a single line within an analyzed source file or pull request diff hunk.
 */
public class AnalyzedLine {

    private final int lineNumber;
    private final String content;
    private final boolean isAdded;

    public AnalyzedLine(int lineNumber, String content, boolean isAdded) {
        this.lineNumber = lineNumber;
        this.content = content != null ? content : "";
        this.isAdded = isAdded;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return content;
    }

    public boolean isAdded() {
        return isAdded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyzedLine that = (AnalyzedLine) o;
        return lineNumber == that.lineNumber &&
                isAdded == that.isAdded &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, content, isAdded);
    }

    @Override
    public String toString() {
        return "AnalyzedLine{" +
                "lineNumber=" + lineNumber +
                ", isAdded=" + isAdded +
                ", content='" + content + '\'' +
                '}';
    }
}
