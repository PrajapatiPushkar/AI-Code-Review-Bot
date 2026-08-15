package com.pushkar.codereview.github.review.dto;

import java.util.Objects;

public class ReviewFileInput {

    private String filename;
    private String status;
    private int additions;
    private int deletions;
    private int changes;
    private String patch;
    private String previousFilename;

    public ReviewFileInput() {
    }

    public ReviewFileInput(String filename, String status, int additions,
                            int deletions, int changes, String patch, String previousFilename) {
        this.filename = filename;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.patch = patch;
        this.previousFilename = previousFilename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAdditions() {
        return additions;
    }

    public void setAdditions(int additions) {
        this.additions = additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public int getChanges() {
        return changes;
    }

    public void setChanges(int changes) {
        this.changes = changes;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    public String getPreviousFilename() {
        return previousFilename;
    }

    public void setPreviousFilename(String previousFilename) {
        this.previousFilename = previousFilename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewFileInput that = (ReviewFileInput) o;
        return additions == that.additions &&
                deletions == that.deletions &&
                changes == that.changes &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(status, that.status) &&
                Objects.equals(patch, that.patch) &&
                Objects.equals(previousFilename, that.previousFilename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, status, additions, deletions, changes, patch, previousFilename);
    }

    @Override
    public String toString() {
        return "ReviewFileInput{" +
                "filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", changes=" + changes +
                ", patch='" + patch + '\'' +
                ", previousFilename='" + previousFilename + '\'' +
                '}';
    }
}
