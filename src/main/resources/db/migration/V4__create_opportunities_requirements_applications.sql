-- V4: Week 2 tables from the ERD. Requirements go with their DRAFT opportunity; every other foreign key RESTRICTs.

CREATE TABLE opportunities (
                               id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               title        VARCHAR(150)  NOT NULL,
                               description  VARCHAR(2000) NOT NULL,
                               created_by   UUID          NOT NULL,
                               status       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
                               closing_date DATE,
                               created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                               updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                               CONSTRAINT fk_opportunity_creator FOREIGN KEY (created_by) REFERENCES employees (id) ON DELETE RESTRICT,
                               CONSTRAINT ck_opportunity_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
                               CONSTRAINT ck_opportunity_title_length CHECK (char_length(title) BETWEEN 3 AND 150),
                               CONSTRAINT ck_opportunity_description_length CHECK (char_length(description) BETWEEN 10 AND 2000)
);

CREATE INDEX idx_opportunity_status  ON opportunities (status);
CREATE INDEX idx_opportunity_creator ON opportunities (created_by);

CREATE TABLE opportunity_requirements (
                                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          opportunity_id UUID        NOT NULL,
                                          skill_id       UUID        NOT NULL,
                                          minimum_level  VARCHAR(20) NOT NULL,
                                          CONSTRAINT fk_requirement_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities (id) ON DELETE CASCADE,
                                          CONSTRAINT fk_requirement_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE RESTRICT,
                                          CONSTRAINT uq_requirement_opportunity_skill UNIQUE (opportunity_id, skill_id),
                                          CONSTRAINT ck_requirement_level CHECK (minimum_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX idx_requirement_skill ON opportunity_requirements (skill_id);

CREATE TABLE applications (
                              id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              employee_id    UUID        NOT NULL,
                              opportunity_id UUID        NOT NULL,
                              status         VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
                              created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT fk_application_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
                              CONSTRAINT fk_application_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities (id) ON DELETE RESTRICT,
                              CONSTRAINT uq_application_employee_opportunity UNIQUE (employee_id, opportunity_id),
                              CONSTRAINT ck_application_status CHECK (status IN ('SUBMITTED', 'SHORTLISTED', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_application_opportunity ON applications (opportunity_id);