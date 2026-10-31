package com.skillbridge.skill.repository;

import java.util.List;
import java.util.UUID;

import com.skillbridge.skill.domain.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    List<Skill> findAllByOrderByNameAsc();

    List<Skill> findByActiveOrderByNameAsc(boolean active);

    boolean existsByNameIgnoreCase(String name);

    Page<Skill> findByActive(boolean active, Pageable pageable);
}