package com.pushkar.codereview.policy.dto;

import com.pushkar.codereview.github.review.dto.ReviewFindingCategory;
import com.pushkar.codereview.github.review.dto.ReviewFindingSeverity;

public class RuleDefinitionDto {

    private String ruleId;
    private String name;
    private String description;
    private ReviewFindingCategory category;
    private ReviewFindingSeverity defaultSeverity;

    public RuleDefinitionDto() {
    }

    public RuleDefinitionDto(String ruleId, String name, String description, ReviewFindingCategory category, ReviewFindingSeverity defaultSeverity) {
        this.ruleId = ruleId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultSeverity = defaultSeverity;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReviewFindingCategory getCategory() {
        return category;
    }

    public void setCategory(ReviewFindingCategory category) {
        this.category = category;
    }

    public ReviewFindingSeverity getDefaultSeverity() {
        return defaultSeverity;
    }

    public void setDefaultSeverity(ReviewFindingSeverity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }
}
