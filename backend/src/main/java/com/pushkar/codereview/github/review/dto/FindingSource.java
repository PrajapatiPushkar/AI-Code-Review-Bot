package com.pushkar.codereview.github.review.dto;

/**
 * Indicates the origin of a review finding within the hybrid review architecture.
 */
public enum FindingSource {
    /**
     * Finding discovered through LLM-based reasoning (e.g. Google Gemini).
     */
    AI,

    /**
     * Finding discovered through deterministic, rule-based static checks.
     */
    RULE
}
