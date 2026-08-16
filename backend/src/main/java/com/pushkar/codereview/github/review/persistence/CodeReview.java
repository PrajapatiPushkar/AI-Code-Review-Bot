package com.pushkar.codereview.github.review.persistence;

import com.pushkar.codereview.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "code_reviews")
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "installation_id", nullable = false)
    private Long installationId;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "pull_request_number", nullable = false)
    private Integer pullRequestNumber;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "total_findings")
    private Integer totalFindings = 0;

    @Column(name = "posted_comments_count")
    private Integer postedCommentsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CodeReviewStatus status = CodeReviewStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public CodeReview() {
    }

    public CodeReview(Long installationId, String owner, String repository, Integer pullRequestNumber) {
        this(installationId, owner, repository, pullRequestNumber, null, null);
    }

    public CodeReview(Long installationId, String owner, String repository, Integer pullRequestNumber, User user) {
        this(installationId, owner, repository, pullRequestNumber, user, null);
    }

    public CodeReview(Long installationId, String owner, String repository, Integer pullRequestNumber, User user, String commitSha) {
        this.installationId = installationId;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.user = user;
        this.commitSha = commitSha;
        this.status = CodeReviewStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Integer pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public Integer getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(Integer totalFindings) {
        this.totalFindings = totalFindings;
    }

    public Integer getPostedCommentsCount() {
        return postedCommentsCount;
    }

    public void setPostedCommentsCount(Integer postedCommentsCount) {
        this.postedCommentsCount = postedCommentsCount;
    }

    public CodeReviewStatus getStatus() {
        return status;
    }

    public void setStatus(CodeReviewStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReview that = (CodeReview) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CodeReview{" +
                "id=" + id +
                ", installationId=" + installationId +
                ", owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
