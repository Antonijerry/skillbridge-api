package com.skillbridge.security;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityBasicsTest extends AbstractIntegrationTest {

    private static final String JAVA = "aaaaaaaa-0000-0000-0000-000000000001";

    @Autowired
    MockMvc mvc;

    @Test
    void sqlInjectionLookingEmailIsJustAnUnknownLogin() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAuth.loginJson("' OR '1'='1", "anything")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void markupInTextIsStoredAsDataAndServedAsJsonWithNosniff() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String markup = "<script>alert(1)</script>";

        mvc.perform(post("/api/skill-assessments").header("Authorization", employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":\"" + JAVA + "\",\"claimedLevel\":\"BEGINNER\","
                                + "\"evidenceSummary\":\"" + markup + "\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.evidenceSummary").value(markup));
    }

    @Test
    void badInputNeverLeaksJavaInternals() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        String body = mvc.perform(get("/api/opportunities/not-a-uuid").header("Authorization", employee))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Exception", "java.", "at com.", "org.springframework");
    }

    @Test
    void withNoConfiguredOriginsNoBrowserOriginIsAllowed() throws Exception {
        mvc.perform(options("/api/skills")
                        .header("Origin", "https://some-site.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}