package com.pushkar.codereview.analytics.dto;

/**
 * Factual status describing repository review health and activity.
 * Does not compute arbitrary security scores or assume security from zero findings.
 */
public enum RepositoryHealthStatus {
    /**
     * No reviews have been performed or recorded for this repository.
     */
    NO_REVIEWS("No reviews available"),

    /**
     * Reviews were completed with zero findings detected.
     * Note: This indicates absence of recorded issues in analyzed PRs, not an absolute security guarantee.
     */
    NO_FINDINGS_RECORDED("No findings recorded"),

    /**
     * Reviews were completed with one or more findings identified.
     */
    COMPLETED_WITH_FINDINGS("Reviews completed with findings");

    private final String description;

    RepositoryHealthStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
