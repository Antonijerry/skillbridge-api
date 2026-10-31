package com.skillbridge.assessment;

import java.util.UUID;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentRulesTest extends AbstractIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    // Seeded, read-only skills (V3)
    private static final String JAVA = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SQL = "aaaaaaaa-0000-0000-0000-000000000002";
    private static final String REST = "aaaaaaaa-0000-0000-0000-000000000003";
    private static final String DOCKER = "aaaaaaaa-0000-0000-0000-000000000005";

    @Autowired
    MockMvc mvc;

    // ---------- helpers ----------

    private String manager() throws Exception {
        return TestAuth.bearer(TestAuth.tokenFor(mvc, TestAuth.TUNDE));
    }

    private String claimJson(String skillId, String level) {
        return "{\"skillId\":\"" + skillId + "\",\"claimedLevel\":\"" + level + "\","
                + "\"evidenceSummary\":\"Built and shipped a small project using this skill.\","
                + "\"evidenceUrl\":\"https://example.com/proof\"}";
    }

    private ResultActions claim(String bearer, String skillId, String level) throws Exception {
        return mvc.perform(post("/api/skill-assessments").header(AUTH, bearer)
                .contentType(JSON).content(claimJson(skillId, level)));
    }

    private String claimOk(String bearer, String skillId, String level) throws Exception {
        return TestAuth.idOf(claim(bearer, skillId, level).andExpect(status().is2xxSuccessful()));
    }

    private ResultActions verify(String bearer, String id) throws Exception {
        return mvc.perform(patch("/api/skill-assessments/{id}/verify", id).header(AUTH, bearer)
                .contentType(JSON).content("{}"));
    }

    private ResultActions reject(String bearer, String id, String comment) throws Exception {
        return mvc.perform(patch("/api/skill-assessments/{id}/reject", id).header(AUTH, bearer)
                .contentType(JSON).content("{\"reviewComment\":\"" + comment + "\"}"));
    }

    // ---------- creating claims ----------

    @Test
    void secondPendingClaimForTheSameSkillReturns409() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        claimOk(employee, JAVA, "BEGINNER");

        claim(employee, JAVA, "ADVANCED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PENDING_CLAIM_EXISTS"));
    }

    @Test
    void claimAtOrBelowAnAlreadyVerifiedLevelReturns409() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(employee, JAVA, "INTERMEDIATE");
        verify(manager(), id).andExpect(status().isOk());

        claim(employee, JAVA, "INTERMEDIATE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_VERIFIED"));
        claim(employee, JAVA, "BEGINNER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_VERIFIED"));
    }

    @Test
    void claimAboveAnAlreadyVerifiedLevelIsAllowed() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(employee, JAVA, "INTERMEDIATE");
        verify(manager(), id).andExpect(status().isOk());

        claim(employee, JAVA, "EXPERT").andExpect(status().is2xxSuccessful());
    }

    @Test
    void claimForAnUnknownSkillReturns404() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        claim(employee, UUID.randomUUID().toString(), "BEGINNER")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void invalidEvidenceReturns400() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        mvc.perform(post("/api/skill-assessments").header(AUTH, employee).contentType(JSON)
                        .content("{\"skillId\":\"" + SQL + "\",\"claimedLevel\":\"BEGINNER\","
                                + "\"evidenceSummary\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/skill-assessments").header(AUTH, employee).contentType(JSON)
                        .content("{\"skillId\":\"" + SQL + "\",\"claimedLevel\":\"BEGINNER\","
                                + "\"evidenceSummary\":\"A long enough summary of the evidence.\","
                                + "\"evidenceUrl\":\"ftp://example.com/x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ---------- reviewing claims ----------

    @Test
    void employeeCannotVerifyEvenWhenTheClaimDoesNotExist() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);

        verify(employee, UUID.randomUUID().toString())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void managerCannotVerifyTheirOwnClaim() throws Exception {
        String manager = manager();
        String id = claimOk(manager, REST, "BEGINNER");

        verify(manager, id)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void verifyingTwiceReturns409() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(employee, DOCKER, "BEGINNER");
        verify(manager(), id).andExpect(status().isOk());

        verify(manager(), id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ASSESSMENT_NOT_EDITABLE"));
    }

    @Test
    void rejectionWithoutACommentReturns400() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(employee, SQL, "BEGINNER");

        reject(manager(), id, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectedClaimCanBeMadeAgain() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(employee, SQL, "ADVANCED");

        reject(manager(), id, "The evidence does not show this level.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        claim(employee, SQL, "INTERMEDIATE").andExpect(status().is2xxSuccessful());
    }

    // ---------- ownership and visibility ----------

    @Test
    void anotherEmployeeCannotReadOrWithdrawMyClaim() throws Exception {
        String owner = TestAuth.newEmployeeBearer(mvc);
        String other = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(owner, JAVA, "BEGINNER");

        mvc.perform(get("/api/skill-assessments/{id}", id).header(AUTH, other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/skill-assessments/{id}", id).header(AUTH, other))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanWithdrawAPendingClaimAndItIsGone() throws Exception {
        String owner = TestAuth.newEmployeeBearer(mvc);
        String id = claimOk(owner, JAVA, "BEGINNER");

        mvc.perform(delete("/api/skill-assessments/{id}", id).header(AUTH, owner))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/skill-assessments/{id}", id).header(AUTH, owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void employeeListOnlyEverContainsTheirOwnClaims() throws Exception {
        String employee = TestAuth.newEmployeeBearer(mvc);
        claimOk(employee, JAVA, "BEGINNER");
        claimOk(TestAuth.newEmployeeBearer(mvc), JAVA, "BEGINNER"); // someone else's claim

        // Asking for another employee's id must not leak their rows.
        mvc.perform(get("/api/skill-assessments").param("employeeId", UUID.randomUUID().toString())
                        .header(AUTH, employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}