package com.pushkar.codereview.github.review.dto;

import java.util.Objects;

public class CodeReviewExecutionResult {

    private String repository;
    private Long pullRequestNumber;
    private String reviewSummary;
    private int totalFindings;
    private int postedCommentsCount;

    public CodeReviewExecutionResult() {
    }

    public CodeReviewExecutionResult(String repository, Long pullRequestNumber, String reviewSummary, int totalFindings, int postedCommentsCount) {
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.reviewSummary = reviewSummary;
        this.totalFindings = totalFindings;
        this.postedCommentsCount = postedCommentsCount;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Long pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(int totalFindings) {
        this.totalFindings = totalFindings;
    }

    public int getPostedCommentsCount() {
        return postedCommentsCount;
    }

    public void setPostedCommentsCount(int postedCommentsCount) {
        this.postedCommentsCount = postedCommentsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReviewExecutionResult that = (CodeReviewExecutionResult) o;
        return totalFindings == that.totalFindings &&
                postedCommentsCount == that.postedCommentsCount &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(reviewSummary, that.reviewSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, pullRequestNumber, reviewSummary, totalFindings, postedCommentsCount);
    }

    @Override
    public String toString() {
        return "CodeReviewExecutionResult{" +
                "repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", reviewSummary='" + reviewSummary + '\'' +
                ", totalFindings=" + totalFindings +
                ", postedCommentsCount=" + postedCommentsCount +
                '}';
    }
}
