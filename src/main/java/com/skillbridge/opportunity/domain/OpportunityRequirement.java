package com.skillbridge.opportunity.domain;

import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One row per (opportunity, skill). Owned by its Opportunity: never created or removed on its own. */
@Entity
@Table(name = "opportunity_requirements")
public class OpportunityRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_level", nullable = false)
    private ClaimedLevel minimumLevel;

    protected OpportunityRequirement() { }

    OpportunityRequirement(Opportunity opportunity, UUID skillId, ClaimedLevel minimumLevel) {
        this.opportunity = opportunity;
        this.skillId = skillId;
        this.minimumLevel = minimumLevel;
    }

    public UUID getId() { return id; }
    public UUID getSkillId() { return skillId; }
    public ClaimedLevel getMinimumLevel() { return minimumLevel; }
}