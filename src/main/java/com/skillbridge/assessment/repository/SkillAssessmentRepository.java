package com.skillbridge.assessment.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.domain.ClaimedLevel;
import com.skillbridge.assessment.domain.SkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SkillAssessmentRepository
        extends JpaRepository<SkillAssessment, UUID>, JpaSpecificationExecutor<SkillAssessment> {

    boolean existsByEmployeeIdAndSkillIdAndStatus(UUID employeeId, UUID skillId, AssessmentStatus status);

    boolean existsByEmployeeIdAndSkillIdAndStatusAndClaimedLevelIn(
            UUID employeeId, UUID skillId, AssessmentStatus status, Collection<ClaimedLevel> levels);
    List<SkillAssessment> findByEmployeeIdAndStatusAndSkillIdIn(UUID employeeId, AssessmentStatus status,
                                                                Collection<UUID> skillIds);
}
