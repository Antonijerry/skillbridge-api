package com.skillbridge.assessment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimedLevelTest {

    /** MatchService compares levels with compareTo, so declaration order is the business rule. */
    @Test
    void declarationOrderIsTheLevelOrder() {
        assertThat(ClaimedLevel.values()).containsExactly(
                ClaimedLevel.BEGINNER, ClaimedLevel.INTERMEDIATE,
                ClaimedLevel.ADVANCED, ClaimedLevel.EXPERT);
    }
}