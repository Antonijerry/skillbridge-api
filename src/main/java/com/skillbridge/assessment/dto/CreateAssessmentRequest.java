package com.skillbridge.assessment.dto;

import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Limits mirror ck_assessment_summary_length and ck_assessment_url_format in the database. */
public record CreateAssessmentRequest(
        @NotNull(message = "is required") UUID employeeId,
        @NotNull(message = "is required") UUID skillId,
        @NotNull(message = "is required") ClaimedLevel claimedLevel,
        @NotNull(message = "is required")
        @Size(min = 10, max = 1000, message = "must be between 10 and 1000 characters") String evidenceSummary,
        @Size(max = 500, message = "must be at most 500 characters")
        @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://") String evidenceUrl) {

    public CreateAssessmentRequest {
        evidenceSummary = evidenceSummary == null ? null : evidenceSummary.trim();
        evidenceUrl = (evidenceUrl == null || evidenceUrl.isBlank()) ? null : evidenceUrl.trim();
    }
}
