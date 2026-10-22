package com.skillbridge.assessment.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.assessment.domain.SkillAssessment;
import com.skillbridge.assessment.dto.AssessmentResponse;
import com.skillbridge.assessment.dto.CreateAssessmentRequest;
import com.skillbridge.assessment.dto.UpdateAssessmentRequest;
import com.skillbridge.assessment.repository.SkillAssessmentRepository;
import com.skillbridge.common.ApiException;
import com.skillbridge.security.CurrentUser;
import com.skillbridge.skill.domain.Skill;
import com.skillbridge.skill.repository.SkillRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SkillAssessmentService {

    private final SkillAssessmentRepository assessments;
    private final SkillRepository skills;

    public SkillAssessmentService(SkillAssessmentRepository assessments, SkillRepository skills) {
        this.assessments = assessments;
        this.skills = skills;
    }

    public AssessmentResponse create(CurrentUser actor, CreateAssessmentRequest req) {
        UUID employeeId = actor.id();   // identity comes from the token, never from the request body
        Skill skill = skills.findById(req.skillId())
                .orElseThrow(() -> ApiException.notFound("Skill not found"));
        if (!skill.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "SKILL_INACTIVE", "This skill is inactive and cannot be claimed");
        }
        rejectIfAlreadyVerified(employeeId, req.skillId(), req.claimedLevel());
        if (assessments.existsByEmployeeIdAndSkillIdAndStatus(employeeId, req.skillId(), AssessmentStatus.PENDING)) {
            throw ApiException.conflict("PENDING_CLAIM_EXISTS",
                    "A pending claim for this skill already exists. Revise or withdraw it first.");
        }
        SkillAssessment saved = assessments.saveAndFlush(new SkillAssessment(
                employeeId, req.skillId(), req.claimedLevel(), req.evidenceUrl(), req.evidenceSummary()));
        return AssessmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(CurrentUser actor, UUID employeeId, UUID skillId, AssessmentStatus status) {
        // An EMPLOYEE only ever sees their own rows, whatever employeeId they send.
        final UUID effectiveEmployeeId = actor.isEmployee() ? actor.id() : employeeId;
        Specification<SkillAssessment> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (effectiveEmployeeId != null) predicates.add(cb.equal(root.get("employeeId"), effectiveEmployeeId));
            if (skillId != null) predicates.add(cb.equal(root.get("skillId"), skillId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return assessments.findAll(filter, Sort.by("createdAt").ascending()).stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(CurrentUser actor, UUID id) {
        SkillAssessment assessment = find(id);
        if (actor.isEmployee() && !actor.owns(assessment.getEmployeeId())) {
            throw ApiException.forbidden();
        }
        return AssessmentResponse.from(assessment);
    }

    public AssessmentResponse update(CurrentUser actor, UUID id, UpdateAssessmentRequest req) {
        SkillAssessment assessment = find(id);
        requireOwner(actor, assessment);
        requirePending(assessment);
        rejectIfAlreadyVerified(assessment.getEmployeeId(), assessment.getSkillId(), req.claimedLevel());
        assessment.revise(req.claimedLevel(), req.evidenceSummary(), req.evidenceUrl());
        return AssessmentResponse.from(assessments.saveAndFlush(assessment));
    }

    public void delete(CurrentUser actor, UUID id) {
        SkillAssessment assessment = find(id);
        requireOwner(actor, assessment);
        requirePending(assessment);
        assessments.delete(assessment);
    }

    private SkillAssessment find(UUID id) {
        return assessments.findById(id).orElseThrow(() -> ApiException.notFound("Skill assessment not found"));
    }

    /** Changing or withdrawing a claim is for its owner only, whatever the caller's role. */
    private void requireOwner(CurrentUser actor, SkillAssessment assessment) {
        if (!actor.owns(assessment.getEmployeeId())) {
            throw ApiException.forbidden();
        }
    }

    private void requirePending(SkillAssessment assessment) {
        if (assessment.getStatus() != AssessmentStatus.PENDING) {
            throw ApiException.conflict("ASSESSMENT_NOT_EDITABLE",
                    "Only PENDING assessments can be changed or withdrawn");
        }
    }

    /** BR-17: a VERIFIED assessment at the same or a higher level already covers this claim. */
    private void rejectIfAlreadyVerified(UUID employeeId, UUID skillId, ClaimedLevel requested) {
        Set<ClaimedLevel> sameOrHigher = EnumSet.range(requested, ClaimedLevel.EXPERT);
        if (assessments.existsByEmployeeIdAndSkillIdAndStatusAndClaimedLevelIn(
                employeeId, skillId, AssessmentStatus.VERIFIED, sameOrHigher)) {
            throw ApiException.conflict("ALREADY_VERIFIED",
                    "This skill is already verified at the same or a higher level");
        }
    }
}