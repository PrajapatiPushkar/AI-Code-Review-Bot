package com.pushkar.codereview.policy.dto;

import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class RepositoryReviewPolicyRequest {

    private Boolean enabled = true;

    @NotNull(message = "failOnSeverity must not be null")
    private ReviewFindingSeverity failOnSeverity = ReviewFindingSeverity.HIGH;

    private Set<String> enabledRuleIds;

    public RepositoryReviewPolicyRequest() {
    }

    public RepositoryReviewPolicyRequest(Boolean enabled, ReviewFindingSeverity failOnSeverity, Set<String> enabledRuleIds) {
        this.enabled = enabled;
        this.failOnSeverity = failOnSeverity;
        this.enabledRuleIds = enabledRuleIds;
    }

    public Boolean getEnabled() {
        return enabled;
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

    public Set<String> getEnabledRuleIds() {
        return enabledRuleIds;
    }

    public void setEnabledRuleIds(Set<String> enabledRuleIds) {
        this.enabledRuleIds = enabledRuleIds;
    }
}
