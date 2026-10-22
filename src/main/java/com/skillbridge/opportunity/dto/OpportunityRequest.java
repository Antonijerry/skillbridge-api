package com.skillbridge.opportunity.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Limits mirror ck_opportunity_title_length and ck_opportunity_description_length. */
public record OpportunityRequest(
        @NotBlank(message = "is required")
        @Size(min = 3, max = 150, message = "must be between 3 and 150 characters") String title,
        @NotBlank(message = "is required")
        @Size(min = 10, max = 2000, message = "must be between 10 and 2000 characters") String description,
        LocalDate closingDate) {

    public OpportunityRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
    }
}