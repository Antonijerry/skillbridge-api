package com.skillbridge.security;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(3, 60, now::get);

    private MockHttpServletResponse call(String method, String uri, String clientAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(clientAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void allowsTheConfiguredNumberThenReturns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = call("POST", "/api/auth/login", "10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void theLimitResetsWhenTheWindowHasPassed() throws Exception {
        for (int i = 0; i < 4; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        now.addAndGet(60_000);

        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void eachClientAddressHasItsOwnLimit() throws Exception {
        for (int i = 0; i < 4; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        assertThat(call("POST", "/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void onlyPostsToTheAuthRoutesAreLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("GET", "/api/skills", "10.0.0.1").getStatus()).isEqualTo(200);
            assertThat(call("POST", "/api/applications", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }
}