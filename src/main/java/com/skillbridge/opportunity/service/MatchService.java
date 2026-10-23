package com.skillbridge.opportunity.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.assessment.domain.SkillAssessment;
import com.skillbridge.assessment.repository.SkillAssessmentRepository;
import com.skillbridge.common.ApiException;
import com.skillbridge.employee.repository.EmployeeRepository;
import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityRequirement;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.dto.MatchResponse;
import com.skillbridge.opportunity.dto.MatchResponse.RequirementMatch;
import com.skillbridge.opportunity.repository.OpportunityRepository;
import com.skillbridge.security.CurrentUser;
import com.skillbridge.skill.domain.Skill;
import com.skillbridge.skill.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final OpportunityRepository opportunities;
    private final SkillAssessmentRepository assessments;
    private final SkillRepository skills;
    private final EmployeeRepository employees;

    public MatchService(OpportunityRepository opportunities, SkillAssessmentRepository assessments,
                        SkillRepository skills, EmployeeRepository employees) {
        this.opportunities = opportunities;
        this.assessments = assessments;
        this.skills = skills;
        this.employees = employees;
    }

    /** employeeId null means "me". Checking someone else is for MANAGER and ADMIN only. */
    @Transactional(readOnly = true)
    public MatchResponse match(CurrentUser actor, UUID opportunityId, UUID employeeId) {
        UUID target = employeeId == null ? actor.id() : employeeId;
        if (!actor.owns(target) && !actor.isStaff()) {
            throw ApiException.forbidden();
        }
        Opportunity opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> ApiException.notFound("Opportunity not found"));
        if (!actor.isStaff() && opportunity.getStatus() != OpportunityStatus.PUBLISHED) {
            throw ApiException.forbidden();
        }
        if (!employees.existsById(target)) {
            throw ApiException.notFound("Employee not found");
        }

        List<OpportunityRequirement> requirements = opportunity.getRequirements();
        Set<UUID> skillIds = requirements.stream().map(OpportunityRequirement::getSkillId)
                .collect(Collectors.toSet());

        // Highest VERIFIED level per skill. PENDING and REJECTED claims are never read.
        Map<UUID, ClaimedLevel> best = new HashMap<>();
        if (!skillIds.isEmpty()) {
            for (SkillAssessment a : assessments.findByEmployeeIdAndStatusAndSkillIdIn(
                    target, AssessmentStatus.VERIFIED, skillIds)) {
                best.merge(a.getSkillId(), a.getClaimedLevel(), (x, y) -> x.compareTo(y) >= 0 ? x : y);
            }
        }
        Map<UUID, String> names = skills.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));

        List<RequirementMatch> rows = requirements.stream().map(r -> {
            ClaimedLevel verified = best.get(r.getSkillId());
            boolean met = verified != null && verified.compareTo(r.getMinimumLevel()) >= 0;
            return new RequirementMatch(r.getSkillId(), names.get(r.getSkillId()), r.getMinimumLevel(), verified, met);
        }).toList();

        int met = (int) rows.stream().filter(RequirementMatch::met).count();
        int percentage = rows.isEmpty() ? 0 : (met * 100) / rows.size();   // integer division rounds down (BR-12)
        return new MatchResponse(opportunityId, target, rows.size(), met, percentage, rows);
    }
}