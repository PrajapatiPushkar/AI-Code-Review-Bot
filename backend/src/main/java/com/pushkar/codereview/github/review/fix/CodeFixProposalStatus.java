package com.pushkar.codereview.github.review.fix;

public enum CodeFixProposalStatus {
    PROPOSED,
    REVIEWED,
    REJECTED,
    EXPIRED;

    /**
     * Determines whether transitioning from this status to target status is valid.
     * Allowed transitions:
     * - PROPOSED -> REVIEWED
     * - PROPOSED -> REJECTED
     * - PROPOSED -> EXPIRED
     * - REVIEWED -> EXPIRED
     *
     * All other transitions (including REJECTED -> *, EXPIRED -> *, self-transitions) are disallowed.
     */
    public boolean canTransitionTo(CodeFixProposalStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PROPOSED -> target == REVIEWED || target == REJECTED || target == EXPIRED;
            case REVIEWED -> target == EXPIRED;
            case REJECTED, EXPIRED -> false;
        };
    }
}
