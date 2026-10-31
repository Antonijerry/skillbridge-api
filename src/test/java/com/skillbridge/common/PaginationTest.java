package com.skillbridge.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationTest extends AbstractIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final String JAVA = "aaaaaaaa-0000-0000-0000-000000000001";

    @Autowired
    MockMvc mvc;

    @Test
    void skillListIsAnArrayWithATotalCountHeader() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(get("/api/skills").header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void sizeLimitsTheNumberOfItems() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(get("/api/skills").param("size", "2").header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void walkingEveryPageVisitsEachSkillExactlyOnce() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        List<String> seen = new ArrayList<>();
        long total = -1;

        for (int page = 0; page < 100; page++) {
            MvcResult result = mvc.perform(get("/api/skills")
                            .param("size", "2").param("page", String.valueOf(page)).header(AUTH, employee))
                    .andExpect(status().isOk())
                    .andReturn();
            total = Long.parseLong(result.getResponse().getHeader("X-Total-Count"));
            List<String> ids = JsonPath.read(result.getResponse().getContentAsString(), "$[*].id");
            if (ids.isEmpty()) {
                break;
            }
            seen.addAll(ids);
        }

        assertThat(seen).doesNotHaveDuplicates();
        assertThat(seen).hasSize((int) total);
    }

    @Test
    void pageBeyondTheEndReturnsAnEmptyArrayAndStillReportsTheTotal() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(get("/api/skills").param("page", "9999").header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidPagingValuesReturn400() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        for (String[] bad : new String[][] {{"size", "0"}, {"size", "101"}, {"page", "-1"}}) {
            mvc.perform(get("/api/skills").param(bad[0], bad[1]).header(AUTH, employee))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void nonNumericPagingValueReturns400() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(get("/api/skills").param("size", "abc").header(AUTH, employee))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void opportunityPageIsNewestFirstAndStillCarriesItsRequirements() throws Exception {
        String manager = TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.TUNDE));

        String id = TestAuth.idOf(mvc.perform(post("/api/opportunities").header(AUTH, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pagination check\","
                                + "\"description\":\"Created by the pagination test.\","
                                + "\"closingDate\":\"" + LocalDate.now().plusDays(30) + "\"}"))
                .andExpect(status().is2xxSuccessful()));
        mvc.perform(post("/api/opportunities/{id}/requirements", id).header(AUTH, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":\"" + JAVA + "\",\"minimumLevel\":\"BEGINNER\"}"))
                .andExpect(status().is2xxSuccessful());

        // The newest opportunity is first, and its requirement was loaded for the page.
        mvc.perform(get("/api/opportunities").param("size", "1").header(AUTH, manager))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].requirements.length()").value(1));
    }
}
