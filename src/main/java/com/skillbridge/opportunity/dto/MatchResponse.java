package com.skillbridge.opportunity.dto;

import java.util.List;
import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;

public record MatchResponse(UUID opportunityId, UUID employeeId, int totalRequirements, int requirementsMet,
                            int matchPercentage, List<RequirementMatch> requirements) {

    /** verifiedLevel is the highest VERIFIED level the employee has for the skill, or null if none. */
    public record RequirementMatch(UUID skillId, String skillName, ClaimedLevel minimumLevel,
                                   ClaimedLevel verifiedLevel, boolean met) { }
}