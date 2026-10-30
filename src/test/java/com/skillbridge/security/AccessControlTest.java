package com.skillbridge.security;

import java.util.UUID;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String employeeAuth() throws Exception {
        return TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.ADA));
    }

    private String managerAuth() throws Exception {
        return TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.TUNDE));
    }

    @Test
    void validTokenReachesProtectedEndpoint() throws Exception {
        mvc.perform(get("/api/employees/me").header("Authorization", employeeAuth()))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotPublishOpportunity() throws Exception {
        // Role is checked before the lookup, so a random id is still 403, not 404.
        mvc.perform(patch("/api/opportunities/{id}/publish", UUID.randomUUID())
                        .header("Authorization", employeeAuth()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void employeeCannotDeleteOpportunity() throws Exception {
        mvc.perform(delete("/api/opportunities/{id}", UUID.randomUUID())
                        .header("Authorization", employeeAuth()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void managerGets404ForUnknownOpportunityOnPublish() throws Exception {
        mvc.perform(patch("/api/opportunities/{id}/publish", UUID.randomUUID())
                        .header("Authorization", managerAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void managerGets404ForUnknownOpportunityOnRead() throws Exception {
        mvc.perform(get("/api/opportunities/{id}", UUID.randomUUID())
                        .header("Authorization", managerAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void unknownRouteReturns404InOurErrorFormat() throws Exception {
        mvc.perform(get("/api/does-not-exist").header("Authorization", employeeAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void wrongHttpMethodReturns405InOurErrorFormat() throws Exception {
        mvc.perform(delete("/api/skills").header("Authorization", employeeAuth()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }
}