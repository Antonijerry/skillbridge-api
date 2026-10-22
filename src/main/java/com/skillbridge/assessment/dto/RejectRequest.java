package com.skillbridge.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A rejection must say why. */
public record RejectRequest(
        @NotBlank(message = "is required")
        @Size(max = 500, message = "must be at most 500 characters") String reviewComment) {

    public RejectRequest {
        reviewComment = reviewComment == null ? null : reviewComment.trim();
    }
}