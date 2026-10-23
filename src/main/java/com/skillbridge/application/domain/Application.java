package com.skillbridge.application.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** An employee's application to an opportunity. Both sides are referenced by id (design decision 20). */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "opportunity_id", nullable = false, updatable = false)
    private UUID opportunityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Application() { }

    public static Application submit(UUID employeeId, UUID opportunityId) {
        Application a = new Application();
        a.employeeId = employeeId;
        a.opportunityId = opportunityId;
        a.status = ApplicationStatus.SUBMITTED;
        return a;
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

    /** The service has already checked the transition is allowed. */
    public void shortlist() { this.status = ApplicationStatus.SHORTLISTED; }

    public void reject() { this.status = ApplicationStatus.REJECTED; }

    public void withdraw() { this.status = ApplicationStatus.WITHDRAWN; }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getOpportunityId() { return opportunityId; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}