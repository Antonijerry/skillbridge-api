-- V2: allow assessment history per (employee, skill), but only one open claim.
-- V1 had UNIQUE (employee_id, skill_id), which dead-ends rejected employees.

ALTER TABLE skill_assessments
DROP CONSTRAINT uq_assessment_employee_skill;

-- Lookup index for "all assessments of this employee for this skill".
CREATE INDEX ix_assessment_employee_skill
    ON skill_assessments (employee_id, skill_id);

-- At most one PENDING claim per employee and skill.
CREATE UNIQUE INDEX uq_assessment_one_pending
    ON skill_assessments (employee_id, skill_id)
    WHERE status = 'PENDING';