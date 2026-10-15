package com.skillbridge.assessment.dto;

import com.skillbridge.assessment.domain.ClaimedLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAssessmentRequest(
        @NotNull(message = "is required") ClaimedLevel claimedLevel,
        @NotNull(message = "is required")
        @Size(min = 10, max = 1000, message = "must be between 10 and 1000 characters") String evidenceSummary,
        @Size(max = 500, message = "must be at most 500 characters")
        @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://") String evidenceUrl) {

    public UpdateAssessmentRequest {
        evidenceSummary = evidenceSummary == null ? null : evidenceSummary.trim();
        evidenceUrl = (evidenceUrl == null || evidenceUrl.isBlank()) ? null : evidenceUrl.trim();
    }
}
