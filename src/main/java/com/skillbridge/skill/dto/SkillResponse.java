package com.skillbridge.skill.dto;

import java.time.Instant;
import java.util.UUID;

import com.skillbridge.skill.domain.Skill;

public record SkillResponse(UUID id, String name, String description, String category,
                            boolean active, Instant createdAt) {

    public static SkillResponse from(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getDescription(), s.getCategory(),
                s.isActive(), s.getCreatedAt());
    }
}