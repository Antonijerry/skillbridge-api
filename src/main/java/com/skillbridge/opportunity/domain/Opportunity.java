package com.skillbridge.opportunity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.skillbridge.assessment.domain.ClaimedLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Aggregate root: it owns its requirements. The creator is referenced by id. */
@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStatus status;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpportunityRequirement> requirements = new ArrayList<>();

    protected Opportunity() { }

    public static Opportunity draft(UUID createdBy, String title, String description, LocalDate closingDate) {
        Opportunity o = new Opportunity();
        o.createdBy = createdBy;
        o.title = title;
        o.description = description;
        o.closingDate = closingDate;
        o.status = OpportunityStatus.DRAFT;
        return o;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** The service has already checked the status is DRAFT. */
    public void revise(String title, String description, LocalDate closingDate) {
        this.title = title;
        this.description = description;
        this.closingDate = closingDate;
    }

    public void publish() { this.status = OpportunityStatus.PUBLISHED; }

    public void close() { this.status = OpportunityStatus.CLOSED; }

    public boolean hasRequirementFor(UUID skillId) {
        return requirements.stream().anyMatch(r -> r.getSkillId().equals(skillId));
    }

    public OpportunityRequirement addRequirement(UUID skillId, ClaimedLevel minimumLevel) {
        OpportunityRequirement requirement = new OpportunityRequirement(this, skillId, minimumLevel);
        requirements.add(requirement);
        return requirement;
    }

    public void removeRequirement(OpportunityRequirement requirement) {
        requirements.remove(requirement);
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getCreatedBy() { return createdBy; }
    public OpportunityStatus getStatus() { return status; }
    public LocalDate getClosingDate() { return closingDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OpportunityRequirement> getRequirements() { return Collections.unmodifiableList(requirements); }
}