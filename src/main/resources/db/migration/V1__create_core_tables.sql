-- Week 1 tables. Opportunities, requirements and applications arrive in a Week 2 migration.

CREATE TABLE employees (
                           id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           full_name     VARCHAR(120) NOT NULL,
                           email         VARCHAR(160) NOT NULL,
                           password_hash VARCHAR(100) NOT NULL,
                           role          VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
                           active        BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           CONSTRAINT uq_employees_email UNIQUE (email),
                           CONSTRAINT ck_employees_email_lower CHECK (email = lower(email)),
                           CONSTRAINT ck_employees_role CHECK (role IN ('EMPLOYEE', 'MANAGER', 'ADMIN'))
);

CREATE TABLE skills (
                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name        VARCHAR(80)  NOT NULL,
                        description VARCHAR(500),
                        category    VARCHAR(60)  NOT NULL,
                        active      BOOLEAN      NOT NULL DEFAULT TRUE,
                        created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Skill names are unique regardless of case: "Java" and "java" cannot both exist.
CREATE UNIQUE INDEX uq_skills_name_lower ON skills (lower(name));

CREATE TABLE skill_assessments (
                                   id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   employee_id      UUID         NOT NULL,
                                   skill_id         UUID         NOT NULL,
                                   claimed_level    VARCHAR(20)  NOT NULL,
                                   evidence_url     VARCHAR(500),
                                   evidence_summary VARCHAR(1000) NOT NULL,
                                   status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                   reviewer_id      UUID,
                                   reviewed_at      TIMESTAMPTZ,
                                   review_comment   VARCHAR(500),
                                   created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                   updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

                                   CONSTRAINT fk_assessment_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_assessment_reviewer FOREIGN KEY (reviewer_id) REFERENCES employees (id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_assessment_skill    FOREIGN KEY (skill_id)    REFERENCES skills (id)    ON DELETE RESTRICT,

                                   CONSTRAINT uq_assessment_employee_skill UNIQUE (employee_id, skill_id),

                                   CONSTRAINT ck_assessment_level CHECK (claimed_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
                                   CONSTRAINT ck_assessment_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
                                   CONSTRAINT ck_assessment_summary_length CHECK (char_length(evidence_summary) BETWEEN 10 AND 1000),
                                   CONSTRAINT ck_assessment_url_format CHECK (evidence_url IS NULL OR evidence_url ~ '^https?://.+'),
    CONSTRAINT ck_assessment_no_self_review CHECK (reviewer_id IS NULL OR reviewer_id <> employee_id),
    CONSTRAINT ck_assessment_review_state CHECK (
        (status = 'PENDING' AND reviewer_id IS NULL AND reviewed_at IS NULL)
        OR (status IN ('VERIFIED', 'REJECTED') AND reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);

CREATE INDEX idx_assessment_status ON skill_assessments (status);
CREATE INDEX idx_assessment_skill  ON skill_assessments (skill_id);