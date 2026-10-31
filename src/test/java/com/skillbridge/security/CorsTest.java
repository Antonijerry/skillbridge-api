package com.skillbridge.security;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "skillbridge.cors.allowed-origins=https://app.example.com")
class CorsTest extends AbstractIntegrationTest {

    private static final String ALLOWED = "https://app.example.com";

    @Autowired
    MockMvc mvc;

    @Test
    void preflightFromAnAllowedOriginIsAccepted() throws Exception {
        mvc.perform(options("/api/skills")
                        .header("Origin", ALLOWED)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED));
    }

    @Test
    void preflightFromAnyOtherOriginIsRejected() throws Exception {
        mvc.perform(options("/api/skills")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAllowedOriginCanReadTheTotalCountHeader() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(get("/api/skills").header("Authorization", employee).header("Origin", ALLOWED))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Total-Count")));
    }
}