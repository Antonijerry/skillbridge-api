package com.skillbridge.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotBlank(message = "is required")
        @Size(min = 2, max = 80, message = "must be between 2 and 80 characters") String name,
        @NotBlank(message = "is required")
        @Size(max = 60, message = "must be at most 60 characters") String category,
        @Size(max = 500, message = "must be at most 500 characters") String description) {

    public CreateSkillRequest {
        name = name == null ? null : name.trim();
        category = category == null ? null : category.trim();
        description = (description == null || description.isBlank()) ? null : description.trim();
    }
}