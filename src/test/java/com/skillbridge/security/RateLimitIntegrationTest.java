package com.skillbridge.security;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "skillbridge.rate-limit.auth.max-requests=3")
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void theFourthAuthRequestInTheWindowReturns429InOurErrorFormat() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(TestAuth.loginJson("nobody@example.com", "Wrong-Password-1")))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAuth.loginJson("nobody@example.com", "Wrong-Password-1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }
}