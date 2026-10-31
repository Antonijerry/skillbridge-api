package com.skillbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.skillbridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class OpenApiDocsTest extends AbstractIntegrationTest {

    @Autowired MockMvc docsMvc;

    @Test
    void docs_are_served_without_a_token_and_describe_the_api() throws Exception {
        String json = docsMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(json, "$.openapi")).startsWith("3.");
        assertThat(JsonPath.<Object>read(json, "$.paths['/api/skills']")).isNotNull();
        assertThat(JsonPath.<Object>read(json, "$.components.securitySchemes.bearerAuth")).isNotNull();
    }
}