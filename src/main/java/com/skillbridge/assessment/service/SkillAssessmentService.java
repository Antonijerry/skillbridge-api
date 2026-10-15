package com.skillbridge.assessment.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;



import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.SkillAssessment;
import com.skillbridge.assessment.dto.AssessmentResponse;
import com.skillbridge.assessment.dto.CreateAssessmentRequest;
import com.skillbridge.assessment.dto.UpdateAssessmentRequest;
import com.skillbridge.assessment.repository.SkillAssessmentRepository;
import com.skillbridge.common.ApiException;

import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.employee.repository.EmployeeRepository;
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
    private final EmployeeRepository employees;

    public SkillAssessmentService(SkillAssessmentRepository assessments, SkillRepository skills,
                                  EmployeeRepository employees) {
        this.assessments = assessments;
        this.skills = skills;
        this.employees = employees;
    }

    public AssessmentResponse create(CreateAssessmentRequest req) {
        if (!employees.existsById(req.employeeId())) {
            throw ApiException.notFound("Employee not found");
        }
        Skill skill = skills.findById(req.skillId())
                .orElseThrow(() -> ApiException.notFound("Skill not found"));
        if (!skill.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "SKILL_INACTIVE", "This skill is inactive and cannot be claimed");
        }
        rejectIfAlreadyVerified(req.employeeId(), req.skillId(), req.claimedLevel());
        if (assessments.existsByEmployeeIdAndSkillIdAndStatus(
                req.employeeId(), req.skillId(), AssessmentStatus.PENDING)) {
            throw ApiException.conflict("PENDING_CLAIM_EXISTS",
                    "A pending claim for this skill already exists. Revise or withdraw it first.");
        }
        SkillAssessment saved = assessments.saveAndFlush(new SkillAssessment(
                req.employeeId(), req.skillId(), req.claimedLevel(), req.evidenceUrl(), req.evidenceSummary()));
        return AssessmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID employeeId, UUID skillId, AssessmentStatus status) {
        Specification<SkillAssessment> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) predicates.add(cb.equal(root.get("employeeId"), employeeId));
            if (skillId != null) predicates.add(cb.equal(root.get("skillId"), skillId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return assessments.findAll(filter, Sort.by("createdAt").ascending()).stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(UUID id) {
        return AssessmentResponse.from(find(id));
    }

    public AssessmentResponse update(UUID id, UpdateAssessmentRequest req) {
        SkillAssessment assessment = find(id);
        requirePending(assessment);
        rejectIfAlreadyVerified(assessment.getEmployeeId(), assessment.getSkillId(), req.claimedLevel());
        assessment.revise(req.claimedLevel(), req.evidenceSummary(), req.evidenceUrl());
        return AssessmentResponse.from(assessments.saveAndFlush(assessment));
    }

    public void delete(UUID id) {
        SkillAssessment assessment = find(id);
        requirePending(assessment);
        assessments.delete(assessment);
    }

    private SkillAssessment find(UUID id) {
        return assessments.findById(id).orElseThrow(() -> ApiException.notFound("Skill assessment not found"));
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
