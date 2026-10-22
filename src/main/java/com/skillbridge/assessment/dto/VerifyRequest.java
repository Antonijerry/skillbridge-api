package com.skillbridge.assessment.dto;

import jakarta.validation.constraints.Size;

/** The whole body is optional for verify. */
public record VerifyRequest(
        @Size(max = 500, message = "must be at most 500 characters") String reviewComment) {

    public VerifyRequest {
        reviewComment = (reviewComment == null || reviewComment.isBlank()) ? null : reviewComment.trim();
    }
}