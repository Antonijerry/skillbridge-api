package com.skillbridge.opportunity.dto;

import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;
import jakarta.validation.constraints.NotNull;

public record AddRequirementRequest(
        @NotNull(message = "is required") UUID skillId,
        @NotNull(message = "is required") ClaimedLevel minimumLevel) {
}