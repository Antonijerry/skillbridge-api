package com.skillbridge.assessment.dto;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.assessment.domain.SkillAssessment;

import java.time.Instant;
import java.util.UUID;

/** API contract for a skill assessment. The JPA entity is never returned directly. */
public record AssessmentResponse(
        UUID id,
        UUID employeeId,
        UUID skillId,
        ClaimedLevel claimedLevel,
        String evidenceUrl,
        String evidenceSummary,
        AssessmentStatus status,
        UUID reviewerId,
        Instant reviewedAt,
        String reviewComment,
        Instant createdAt,
        Instant updatedAt) {

    public static AssessmentResponse from(SkillAssessment a) {
        return new AssessmentResponse(a.getId(), a.getEmployeeId(), a.getSkillId(), a.getClaimedLevel(),
                a.getEvidenceUrl(), a.getEvidenceSummary(), a.getStatus(), a.getReviewerId(),
                a.getReviewedAt(), a.getReviewComment(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
