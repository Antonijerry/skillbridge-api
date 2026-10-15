package com.skillbridge.skill.repository;

import java.util.UUID;

import com.skillbridge.skill.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
}
