package com.skillbridge.assessment.domain;

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

@Entity
@Table(name = "skill_assessments")
public class SkillAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "skill_id", nullable = false, updatable = false)
    private UUID skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "claimed_level", nullable = false)
    private ClaimedLevel claimedLevel;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(name = "evidence_summary", nullable = false)
    private String evidenceSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillAssessment() { }

    public SkillAssessment(UUID employeeId, UUID skillId, ClaimedLevel claimedLevel,
                           String evidenceUrl, String evidenceSummary) {
        this.employeeId = employeeId;
        this.skillId = skillId;
        this.claimedLevel = claimedLevel;
        this.evidenceUrl = evidenceUrl;
        this.evidenceSummary = evidenceSummary;
        this.status = AssessmentStatus.PENDING;
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

    /** Replace the editable fields. The caller has already checked the status is PENDING. */
    public void revise(ClaimedLevel claimedLevel, String evidenceSummary, String evidenceUrl) {
        this.claimedLevel = claimedLevel;
        this.evidenceSummary = evidenceSummary;
        this.evidenceUrl = evidenceUrl;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getSkillId() { return skillId; }
    public ClaimedLevel getClaimedLevel() { return claimedLevel; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public AssessmentStatus getStatus() { return status; }
    public UUID getReviewerId() { return reviewerId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewComment() { return reviewComment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
