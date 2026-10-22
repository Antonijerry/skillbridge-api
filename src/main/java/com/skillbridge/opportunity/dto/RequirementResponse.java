package com.skillbridge.opportunity.dto;

import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;

public record RequirementResponse(UUID id, UUID skillId, String skillName, ClaimedLevel minimumLevel) { }