package com.pushkar.codereview.policy;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import com.pushkar.codereview.repository.Repository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "repository_review_policies")
public class RepositoryReviewPolicy {

    public static final String DEFAULT_RULES = "RULE-JAVA-SYSTEM-OUT,RULE-JAVA-EMPTY-CATCH,RULE-TODO-FIXME";
    public static final ReviewFindingSeverity DEFAULT_SEVERITY = ReviewFindingSeverity.HIGH;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false, unique = true)
    private Repository repository;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_on_severity", nullable = false, length = 20)
    private ReviewFindingSeverity failOnSeverity = DEFAULT_SEVERITY;

    @Column(name = "enabled_rule_ids", nullable = false, length = 1000)
    private String enabledRuleIds = DEFAULT_RULES;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RepositoryReviewPolicy() {
    }

    public RepositoryReviewPolicy(Repository repository, Boolean enabled, ReviewFindingSeverity failOnSeverity, String enabledRuleIds) {
        this.repository = repository;
        this.enabled = enabled != null ? enabled : true;
        this.failOnSeverity = failOnSeverity != null ? failOnSeverity : DEFAULT_SEVERITY;
        this.enabledRuleIds = enabledRuleIds != null ? enabledRuleIds : DEFAULT_RULES;
    }

    public static RepositoryReviewPolicy createDefault(Repository repository) {
        RepositoryReviewPolicy policy = new RepositoryReviewPolicy();
        policy.setRepository(repository);
        policy.setEnabled(true);
        policy.setFailOnSeverity(DEFAULT_SEVERITY);
        policy.setEnabledRuleIds(DEFAULT_RULES);
        Instant now = Instant.now();
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        return policy;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ReviewFindingSeverity getFailOnSeverity() {
        return failOnSeverity;
    }

    public void setFailOnSeverity(ReviewFindingSeverity failOnSeverity) {
        this.failOnSeverity = failOnSeverity;
    }

    public String getEnabledRuleIds() {
        return enabledRuleIds;
    }

    public void setEnabledRuleIds(String enabledRuleIds) {
        this.enabledRuleIds = enabledRuleIds;
    }

    public Set<String> getEnabledRuleIdsSet() {
        if (enabledRuleIds == null || enabledRuleIds.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(enabledRuleIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setEnabledRuleIdsSet(Set<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            this.enabledRuleIds = "";
        } else {
            this.enabledRuleIds = String.join(",", ruleIds);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryReviewPolicy that = (RepositoryReviewPolicy) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
