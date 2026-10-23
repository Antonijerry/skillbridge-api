package com.skillbridge.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.skillbridge.application.domain.Application;
import com.skillbridge.application.domain.ApplicationStatus;

public record ApplicationResponse(UUID id, UUID employeeId, UUID opportunityId, ApplicationStatus status,
                                  Instant createdAt, Instant updatedAt) {

    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(a.getId(), a.getEmployeeId(), a.getOpportunityId(), a.getStatus(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}