package com.skillbridge.opportunity.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.skillbridge.common.ApiException;
import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityRequirement;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.dto.AddRequirementRequest;
import com.skillbridge.opportunity.dto.OpportunityRequest;
import com.skillbridge.opportunity.dto.OpportunityResponse;
import com.skillbridge.opportunity.dto.RequirementResponse;
import com.skillbridge.opportunity.repository.OpportunityRepository;
import com.skillbridge.security.CurrentUser;
import com.skillbridge.skill.domain.Skill;
import com.skillbridge.skill.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OpportunityService {

    private final OpportunityRepository opportunities;
    private final SkillRepository skills;

    public OpportunityService(OpportunityRepository opportunities, SkillRepository skills) {
        this.opportunities = opportunities;
        this.skills = skills;
    }

    public OpportunityResponse create(CurrentUser actor, OpportunityRequest req) {
        if (!actor.isStaff()) {
            throw ApiException.forbidden();
        }
        Opportunity saved = opportunities.saveAndFlush(
                Opportunity.draft(actor.id(), req.title(), req.description(), req.closingDate()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> list(CurrentUser actor, OpportunityStatus status) {
        OpportunityStatus effective = status;
        if (actor.isEmployee()) {
            // An EMPLOYEE only ever sees PUBLISHED opportunities.
            if (status != null && status != OpportunityStatus.PUBLISHED) {
                return List.of();
            }
            effective = OpportunityStatus.PUBLISHED;
        }
        List<Opportunity> found = effective == null
                ? opportunities.findAllByOrderByCreatedAtDesc()
                : opportunities.findByStatusOrderByCreatedAtDesc(effective);
        return toResponses(found);
    }

    @Transactional(readOnly = true)
    public OpportunityResponse get(CurrentUser actor, UUID id) {
        Opportunity opportunity = find(id);
        if (actor.isEmployee() && opportunity.getStatus() != OpportunityStatus.PUBLISHED) {
            throw ApiException.forbidden();
        }
        return toResponse(opportunity);
    }

    public OpportunityResponse update(CurrentUser actor, UUID id, OpportunityRequest req) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.DRAFT, "Only a DRAFT opportunity can be edited");
        opportunity.revise(req.title(), req.description(), req.closingDate());
        return toResponse(opportunities.saveAndFlush(opportunity));
    }

    public void delete(CurrentUser actor, UUID id) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.DRAFT, "Only a DRAFT opportunity can be deleted");
        opportunities.delete(opportunity);   // its requirements go with it
    }

    public RequirementResponse addRequirement(CurrentUser actor, UUID id, AddRequirementRequest req) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.DRAFT, "Requirements can only be changed while the opportunity is DRAFT");
        Skill skill = skills.findById(req.skillId()).orElseThrow(() -> ApiException.notFound("Skill not found"));
        if (!skill.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "SKILL_INACTIVE", "This skill is inactive and cannot be required");
        }
        if (opportunity.hasRequirementFor(skill.getId())) {
            throw ApiException.conflict("REQUIREMENT_EXISTS", "The opportunity already requires this skill");
        }
        OpportunityRequirement added = opportunity.addRequirement(skill.getId(), req.minimumLevel());
        opportunities.saveAndFlush(opportunity);
        return new RequirementResponse(added.getId(), added.getSkillId(), skill.getName(), added.getMinimumLevel());
    }

    public void removeRequirement(CurrentUser actor, UUID id, UUID requirementId) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.DRAFT, "Requirements can only be changed while the opportunity is DRAFT");
        OpportunityRequirement requirement = opportunity.getRequirements().stream()
                .filter(r -> r.getId().equals(requirementId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Requirement not found"));
        opportunity.removeRequirement(requirement);
        opportunities.saveAndFlush(opportunity);
    }

    public OpportunityResponse publish(CurrentUser actor, UUID id) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.DRAFT, "Only a DRAFT opportunity can be published");
        if (opportunity.getRequirements().isEmpty()) {
            throw invalidState("Add at least one requirement before publishing");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (opportunity.getClosingDate() == null || !opportunity.getClosingDate().isAfter(today)) {
            throw invalidState("A closing date in the future is needed to publish");
        }
        opportunity.publish();
        return toResponse(opportunities.saveAndFlush(opportunity));
    }

    public OpportunityResponse close(CurrentUser actor, UUID id) {
        Opportunity opportunity = findManaged(actor, id);
        requireStatus(opportunity, OpportunityStatus.PUBLISHED, "Only a PUBLISHED opportunity can be closed");
        opportunity.close();
        return toResponse(opportunities.saveAndFlush(opportunity));
    }

    private Opportunity find(UUID id) {
        return opportunities.findById(id).orElseThrow(() -> ApiException.notFound("Opportunity not found"));
    }

    /** Role first (403), then existence (404), then ownership (403): creator or ADMIN. */
    private Opportunity findManaged(CurrentUser actor, UUID id) {
        if (!actor.isStaff()) {
            throw ApiException.forbidden();
        }
        Opportunity opportunity = find(id);
        if (!actor.isAdmin() && !actor.owns(opportunity.getCreatedBy())) {
            throw ApiException.forbidden();
        }
        return opportunity;
    }

    private void requireStatus(Opportunity opportunity, OpportunityStatus expected, String message) {
        if (opportunity.getStatus() != expected) {
            throw invalidState(message);
        }
    }

    private ApiException invalidState(String message) {
        return ApiException.conflict("INVALID_STATE", message);
    }

    private OpportunityResponse toResponse(Opportunity opportunity) {
        return toResponses(List.of(opportunity)).get(0);
    }

    /** One query for every skill name across the list. */
    private List<OpportunityResponse> toResponses(List<Opportunity> list) {
        List<UUID> skillIds = list.stream()
                .flatMap(o -> o.getRequirements().stream())
                .map(OpportunityRequirement::getSkillId)
                .distinct()
                .toList();
        Map<UUID, String> names = skills.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));
        return list.stream().map(o -> OpportunityResponse.from(o, names)).toList();
    }
}