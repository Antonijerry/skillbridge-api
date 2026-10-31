package com.skillbridge.common;

import org.junit.jupiter.api.Test;

import static com.skillbridge.support.ApiAssertions.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

class PageParamsTest {

    @Test
    void defaultsAreTheFirstPageOfTwenty() {
        PageParams p = PageParams.of(null, null);
        assertThat(p.page()).isZero();
        assertThat(p.size()).isEqualTo(20);
    }

    @Test
    void acceptsTheMaximumSize() {
        assertThat(PageParams.of(3, 100).size()).isEqualTo(100);
    }

    @Test
    void rejectsNegativePageZeroSizeAndOversizedSize() {
        assertFails(() -> PageParams.of(-1, 10), "page must");
        assertFails(() -> PageParams.of(0, 0), "page must");
        assertFails(() -> PageParams.of(0, 101), "page must");
    }
}