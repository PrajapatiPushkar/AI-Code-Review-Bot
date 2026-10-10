package com.pushkar.codereview.policy.dto;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class RepositoryReviewPolicyResponse {

    private Long id;
    private Long repositoryId;
    private String repositoryName;
    private String repositoryFullName;
    private boolean enabled;
    private ReviewFindingSeverity failOnSeverity;
    private Set<String> enabledRuleIds;
    private List<RuleDefinitionDto> availableRules;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isCustom;

    public RepositoryReviewPolicyResponse() {
    }

    public RepositoryReviewPolicyResponse(Long id, Long repositoryId, String repositoryName, String repositoryFullName,
                                          boolean enabled, ReviewFindingSeverity failOnSeverity,
                                          Set<String> enabledRuleIds, List<RuleDefinitionDto> availableRules,
                                          Instant createdAt, Instant updatedAt, boolean isCustom) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repositoryName = repositoryName;
        this.repositoryFullName = repositoryFullName;
        this.enabled = enabled;
        this.failOnSeverity = failOnSeverity;
        this.enabledRuleIds = enabledRuleIds;
        this.availableRules = availableRules;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isCustom = isCustom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ReviewFindingSeverity getFailOnSeverity() {
        return failOnSeverity;
    }

    public void setFailOnSeverity(ReviewFindingSeverity failOnSeverity) {
        this.failOnSeverity = failOnSeverity;
    }

    public Set<String> getEnabledRuleIds() {
        return enabledRuleIds;
    }

    public void setEnabledRuleIds(Set<String> enabledRuleIds) {
        this.enabledRuleIds = enabledRuleIds;
    }

    public List<RuleDefinitionDto> getAvailableRules() {
        return availableRules;
    }

    public void setAvailableRules(List<RuleDefinitionDto> availableRules) {
        this.availableRules = availableRules;
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

    @com.fasterxml.jackson.annotation.JsonProperty("isCustom")
    public boolean isCustom() {
        return isCustom;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isCustom")
    public void setCustom(boolean custom) {
        isCustom = custom;
    }
}
