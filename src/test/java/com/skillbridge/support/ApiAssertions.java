package com.skillbridge.support;

import com.skillbridge.common.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class ApiAssertions {

    private ApiAssertions() {
    }

    public static void assertForbidden(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .hasMessage(ApiException.forbidden().getMessage());
    }

    public static void assertFails(ThrowingCallable call, String messagePart) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(messagePart);
    }
}