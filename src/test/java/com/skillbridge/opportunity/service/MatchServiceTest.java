package com.skillbridge.opportunity.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.assessment.domain.SkillAssessment;
import com.skillbridge.assessment.repository.SkillAssessmentRepository;
import com.skillbridge.employee.domain.Role;
import com.skillbridge.employee.repository.EmployeeRepository;
import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityRequirement;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.dto.MatchResponse;
import com.skillbridge.opportunity.repository.OpportunityRepository;
import com.skillbridge.security.CurrentUser;
import com.skillbridge.skill.domain.Skill;
import com.skillbridge.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.skillbridge.support.ApiAssertions.assertFails;
import static com.skillbridge.support.ApiAssertions.assertForbidden;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final SkillAssessmentRepository assessments = mock(SkillAssessmentRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final MatchService service = new MatchService(opportunities, assessments, skills, employees);

    private final UUID employeeId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final UUID javaId = UUID.randomUUID();
    private final UUID sqlId = UUID.randomUUID();
    private final UUID dockerId = UUID.randomUUID();

    private final CurrentUser employee = new CurrentUser(employeeId, Role.EMPLOYEE);
    private final CurrentUser manager = new CurrentUser(UUID.randomUUID(), Role.MANAGER);

    @BeforeEach
    void setUp() {
        when(employees.existsById(employeeId)).thenReturn(true);
        List<Skill> known = List.of(skill(javaId, "Java"), skill(sqlId, "SQL"), skill(dockerId, "Docker"));
        when(skills.findAllById(any())).thenReturn(known);
    }

    // ---------- helpers ----------

    private Skill skill(UUID id, String name) {
        Skill s = mock(Skill.class);
        when(s.getId()).thenReturn(id);
        when(s.getName()).thenReturn(name);
        return s;
    }

    private OpportunityRequirement requirement(UUID skillId, ClaimedLevel minimum) {
        OpportunityRequirement r = mock(OpportunityRequirement.class);
        when(r.getSkillId()).thenReturn(skillId);
        when(r.getMinimumLevel()).thenReturn(minimum);
        return r;
    }

    private SkillAssessment verified(UUID skillId, ClaimedLevel level) {
        SkillAssessment a = mock(SkillAssessment.class);
        when(a.getSkillId()).thenReturn(skillId);
        when(a.getClaimedLevel()).thenReturn(level);
        return a;
    }

    private void givenOpportunity(OpportunityStatus status, OpportunityRequirement... requirements) {
        Opportunity o = mock(Opportunity.class);
        when(o.getStatus()).thenReturn(status);
        when(o.getRequirements()).thenReturn(List.of(requirements));
        when(opportunities.findById(opportunityId)).thenReturn(Optional.of(o));
    }

    private void givenVerifiedAssessments(SkillAssessment... found) {
        when(assessments.findByEmployeeIdAndStatusAndSkillIdIn(any(), any(), any()))
                .thenReturn(List.of(found));
    }

    // ---------- match calculation ----------

    @Test
    void verifiedLevelAtTheMinimumMeetsTheRequirement() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.ADVANCED));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.ADVANCED));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirements().get(0).met()).isTrue();
        assertThat(r.requirements().get(0).skillName()).isEqualTo("Java");
        assertThat(r.matchPercentage()).isEqualTo(100);
    }

    @Test
    void verifiedLevelBelowTheMinimumDoesNotMeetTheRequirement() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.ADVANCED));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.INTERMEDIATE));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirements().get(0).met()).isFalse();
        assertThat(r.requirements().get(0).verifiedLevel()).isEqualTo(ClaimedLevel.INTERMEDIATE);
        assertThat(r.matchPercentage()).isZero();
    }

    @Test
    void highestVerifiedLevelWinsWhateverTheOrder() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.ADVANCED));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.BEGINNER), verified(javaId, ClaimedLevel.EXPERT));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirements().get(0).verifiedLevel()).isEqualTo(ClaimedLevel.EXPERT);
        assertThat(r.requirements().get(0).met()).isTrue();
    }

    @Test
    void highestVerifiedLevelWinsWhenTheHighOneComesFirst() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.ADVANCED));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.EXPERT), verified(javaId, ClaimedLevel.BEGINNER));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirements().get(0).verifiedLevel()).isEqualTo(ClaimedLevel.EXPERT);
    }

    @Test
    void noVerifiedAssessmentMeansNullLevelAndNotMet() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.BEGINNER));
        // the repository mock returns an empty list by default

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirements().get(0).verifiedLevel()).isNull();
        assertThat(r.requirements().get(0).met()).isFalse();
    }

    @Test
    void onlyVerifiedAssessmentsAreEverRequested() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.BEGINNER));

        service.match(employee, opportunityId, null);

        verify(assessments).findByEmployeeIdAndStatusAndSkillIdIn(
                eq(employeeId), eq(AssessmentStatus.VERIFIED), any());
    }

    @Test
    void oneOfThreeMetRoundsDownTo33() {
        givenOpportunity(OpportunityStatus.PUBLISHED,
                requirement(javaId, ClaimedLevel.BEGINNER),
                requirement(sqlId, ClaimedLevel.BEGINNER),
                requirement(dockerId, ClaimedLevel.BEGINNER));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.BEGINNER));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.totalRequirements()).isEqualTo(3);
        assertThat(r.requirementsMet()).isEqualTo(1);
        assertThat(r.matchPercentage()).isEqualTo(33);
    }

    @Test
    void twoOfThreeMetRoundsDownTo66() {
        givenOpportunity(OpportunityStatus.PUBLISHED,
                requirement(javaId, ClaimedLevel.BEGINNER),
                requirement(sqlId, ClaimedLevel.BEGINNER),
                requirement(dockerId, ClaimedLevel.BEGINNER));
        givenVerifiedAssessments(verified(javaId, ClaimedLevel.BEGINNER), verified(sqlId, ClaimedLevel.EXPERT));

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.requirementsMet()).isEqualTo(2);
        assertThat(r.matchPercentage()).isEqualTo(66);
    }

    @Test
    void opportunityWithNoRequirementsGivesZeroPercentWithoutDividingByZero() {
        givenOpportunity(OpportunityStatus.PUBLISHED);

        MatchResponse r = service.match(employee, opportunityId, null);

        assertThat(r.totalRequirements()).isZero();
        assertThat(r.matchPercentage()).isZero();
    }

    // ---------- access rules ----------

    @Test
    void employeeCannotCheckAnotherEmployee() {
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.BEGINNER));

        assertForbidden(() -> service.match(employee, opportunityId, UUID.randomUUID()));
    }

    @Test
    void managerCanCheckAnotherEmployee() {
        UUID other = UUID.randomUUID();
        when(employees.existsById(other)).thenReturn(true);
        givenOpportunity(OpportunityStatus.PUBLISHED, requirement(javaId, ClaimedLevel.BEGINNER));

        MatchResponse r = service.match(manager, opportunityId, other);

        assertThat(r.employeeId()).isEqualTo(other);
    }

    @Test
    void employeeCannotMatchAnUnpublishedOpportunity() {
        givenOpportunity(OpportunityStatus.DRAFT);

        assertForbidden(() -> service.match(employee, opportunityId, null));
    }

    @Test
    void managerCanMatchAnUnpublishedOpportunity() {
        UUID other = UUID.randomUUID();
        when(employees.existsById(other)).thenReturn(true);
        givenOpportunity(OpportunityStatus.DRAFT);

        MatchResponse r = service.match(manager, opportunityId, other);

        assertThat(r.opportunityId()).isEqualTo(opportunityId);
    }

    @Test
    void unknownOpportunityIsNotFound() {
        assertFails(() -> service.match(employee, UUID.randomUUID(), null), "Opportunity not found");
    }

    @Test
    void unknownEmployeeIsNotFound() {
        givenOpportunity(OpportunityStatus.PUBLISHED);

        assertFails(() -> service.match(manager, opportunityId, UUID.randomUUID()), "Employee not found");
    }
}