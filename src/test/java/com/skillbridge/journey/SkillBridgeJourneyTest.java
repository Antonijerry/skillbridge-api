package com.skillbridge.journey;

import java.time.LocalDate;
import java.util.UUID;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole product as one story. Also the script for the walkthrough video. */
class SkillBridgeJourneyTest extends AbstractIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Test
    void employeeIsVerifiedThenMatchedThenAppliesIsShortlistedAndWithdraws() throws Exception {
        String admin = TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.CHIDI));
        String manager = TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.TUNDE));
        String employee = TestAuth.newEmployeeBearer(mvc);

        // 1. An admin adds a skill to the catalogue.
        String skillName = "Journey-" + UUID.randomUUID().toString().substring(0, 8);
        String skillId = TestAuth.idOf(mvc.perform(post("/api/skills").header(AUTH, admin)
                        .contentType(JSON)
                        .content("{\"name\":\"" + skillName + "\",\"category\":\"Testing\","
                                + "\"description\":\"Created by the journey test\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.name").value(skillName)));

        // 2. The employee claims ADVANCED. The claim starts PENDING.
        String assessmentId = TestAuth.idOf(mvc.perform(post("/api/skill-assessments").header(AUTH, employee)
                        .contentType(JSON)
                        .content("{\"skillId\":\"" + skillId + "\",\"claimedLevel\":\"ADVANCED\","
                                + "\"evidenceSummary\":\"Led a project that used this skill for a year.\","
                                + "\"evidenceUrl\":\"https://example.com/proof\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("PENDING")));

        // 3. A manager creates an opportunity, requires INTERMEDIATE in that skill, and publishes it.
        String opportunityId = TestAuth.idOf(mvc.perform(post("/api/opportunities").header(AUTH, manager)
                        .contentType(JSON)
                        .content("{\"title\":\"Journey opportunity\","
                                + "\"description\":\"An opportunity created by the journey test.\","
                                + "\"closingDate\":\"" + LocalDate.now().plusDays(30) + "\"}"))
                .andExpect(status().is2xxSuccessful()));

        mvc.perform(post("/api/opportunities/{id}/requirements", opportunityId).header(AUTH, manager)
                        .contentType(JSON)
                        .content("{\"skillId\":\"" + skillId + "\",\"minimumLevel\":\"INTERMEDIATE\"}"))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(patch("/api/opportunities/{id}/publish", opportunityId).header(AUTH, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // 4. Before verification the claim does not count: 0% match.
        mvc.perform(get("/api/opportunities/{id}/match", opportunityId).header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(0));

        // 5. The manager verifies the claim.
        mvc.perform(patch("/api/skill-assessments/{id}/verify", assessmentId).header(AUTH, manager)
                        .contentType(JSON).content("{\"reviewComment\":\"Evidence checked.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // 6. Now the match is 100%.
        mvc.perform(get("/api/opportunities/{id}/match", opportunityId).header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(100))
                .andExpect(jsonPath("$.requirementsMet").value(1));

        // 7. The employee applies.
        String applicationId = TestAuth.idOf(mvc.perform(post("/api/applications").header(AUTH, employee)
                        .contentType(JSON)
                        .content("{\"opportunityId\":\"" + opportunityId + "\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("SUBMITTED")));

        // 8. The manager who owns the opportunity shortlists the application.
        mvc.perform(patch("/api/applications/{id}/shortlist", applicationId).header(AUTH, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        // 9. The employee sees it in their list, then withdraws.
        mvc.perform(get("/api/applications").header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(patch("/api/applications/{id}/withdraw", applicationId).header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }
}