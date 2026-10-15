package com.skillbridge.skill.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of the skills table for Week 1: only the columns the assessment rules need. */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private UUID id;

    private String name;

    private boolean active;

    protected Skill() { }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public boolean isActive() { return active; }
}
