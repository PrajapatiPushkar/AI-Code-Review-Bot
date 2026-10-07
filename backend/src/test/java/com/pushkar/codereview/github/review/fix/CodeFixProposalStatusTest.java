package com.pushkar.codereview.github.review.fix;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeFixProposalStatusTest {

    @Test
    void testAllowedTransitionsFromProposed() {
        assertThat(CodeFixProposalStatus.PROPOSED.canTransitionTo(CodeFixProposalStatus.REVIEWED)).isTrue();
        assertThat(CodeFixProposalStatus.PROPOSED.canTransitionTo(CodeFixProposalStatus.REJECTED)).isTrue();
        assertThat(CodeFixProposalStatus.PROPOSED.canTransitionTo(CodeFixProposalStatus.EXPIRED)).isTrue();
    }

    @Test
    void testAllowedTransitionsFromReviewed() {
        assertThat(CodeFixProposalStatus.REVIEWED.canTransitionTo(CodeFixProposalStatus.EXPIRED)).isTrue();
    }

    @Test
    void testDisallowedTransitionsFromReviewed() {
        assertThat(CodeFixProposalStatus.REVIEWED.canTransitionTo(CodeFixProposalStatus.PROPOSED)).isFalse();
        assertThat(CodeFixProposalStatus.REVIEWED.canTransitionTo(CodeFixProposalStatus.REJECTED)).isFalse();
    }

    @Test
    void testDisallowedTransitionsFromRejected() {
        assertThat(CodeFixProposalStatus.REJECTED.canTransitionTo(CodeFixProposalStatus.PROPOSED)).isFalse();
        assertThat(CodeFixProposalStatus.REJECTED.canTransitionTo(CodeFixProposalStatus.REVIEWED)).isFalse();
        assertThat(CodeFixProposalStatus.REJECTED.canTransitionTo(CodeFixProposalStatus.EXPIRED)).isFalse();
    }

    @Test
    void testDisallowedTransitionsFromExpired() {
        assertThat(CodeFixProposalStatus.EXPIRED.canTransitionTo(CodeFixProposalStatus.PROPOSED)).isFalse();
        assertThat(CodeFixProposalStatus.EXPIRED.canTransitionTo(CodeFixProposalStatus.REVIEWED)).isFalse();
        assertThat(CodeFixProposalStatus.EXPIRED.canTransitionTo(CodeFixProposalStatus.REJECTED)).isFalse();
    }

    @Test
    void testNullAndSelfTransitionsDisallowed() {
        assertThat(CodeFixProposalStatus.PROPOSED.canTransitionTo(null)).isFalse();
        assertThat(CodeFixProposalStatus.PROPOSED.canTransitionTo(CodeFixProposalStatus.PROPOSED)).isFalse();
        assertThat(CodeFixProposalStatus.REVIEWED.canTransitionTo(CodeFixProposalStatus.REVIEWED)).isFalse();
        assertThat(CodeFixProposalStatus.REJECTED.canTransitionTo(CodeFixProposalStatus.REJECTED)).isFalse();
        assertThat(CodeFixProposalStatus.EXPIRED.canTransitionTo(CodeFixProposalStatus.EXPIRED)).isFalse();
    }
}
