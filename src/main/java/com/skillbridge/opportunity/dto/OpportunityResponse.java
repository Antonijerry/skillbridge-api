package com.skillbridge.opportunity.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityStatus;

public record OpportunityResponse(
        UUID id,
        String title,
        String description,
        UUID createdBy,
        OpportunityStatus status,
        LocalDate closingDate,
        Instant createdAt,
        Instant updatedAt,
        List<RequirementResponse> requirements) {

    public static OpportunityResponse from(Opportunity o, Map<UUID, String> skillNames) {
        List<RequirementResponse> requirements = o.getRequirements().stream()
                .map(r -> new RequirementResponse(r.getId(), r.getSkillId(), skillNames.get(r.getSkillId()), r.getMinimumLevel()))
                .sorted(Comparator.comparing(RequirementResponse::skillName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new OpportunityResponse(o.getId(), o.getTitle(), o.getDescription(), o.getCreatedBy(), o.getStatus(),
                o.getClosingDate(), o.getCreatedAt(), o.getUpdatedAt(), requirements);
    }
}