# Entity relationship diagram

Diagram: docs/erd.png (exported from dbdiagram.io). Source of the diagram is the DBML below.

## Relationships
| Relationship | Type | Via |
|---|---|---|
| Employee submits SkillAssessment | one-to-many | skill_assessments.employee_id |
| Employee reviews SkillAssessment | one-to-many (optional) | skill_assessments.reviewer_id |
| Skill is claimed in SkillAssessment | one-to-many | skill_assessments.skill_id |
| Employee creates Opportunity | one-to-many | opportunities.created_by |
| Opportunity requires Skill | **many-to-many** | opportunity_requirements (carries minimum_level) |
| Employee applies to Opportunity | **many-to-many** | applications (carries status) |

## Constraints the database enforces
- skill_assessments: one row per (employee, skill); reviewer_id must differ from employee_id;
  PENDING means no reviewer, VERIFIED or REJECTED means reviewer and reviewed_at are set.
- opportunity_requirements: one row per (opportunity, skill).
- applications: one row per (employee, opportunity).
- Enum-like columns are checked against their allowed values.
- Foreign keys use ON DELETE RESTRICT, except requirements, which go with their DRAFT opportunity.

## DBML (paste into dbdiagram.io)
```dbml
Project skillbridge_api {
  database_type: 'PostgreSQL'
  Note: 'SkillBridge API ERD. Employee Skills Verification and Internal Opportunities Platform. Week 1 main entity: skill_assessments. Primary keys are UUIDs.'
}

Enum employee_role {
  EMPLOYEE
  MANAGER
  ADMIN
}

Enum skill_level {
  BEGINNER
  INTERMEDIATE
  ADVANCED
  EXPERT
}

Enum assessment_status {
  PENDING
  VERIFIED
  REJECTED
}

Enum opportunity_status {
  DRAFT
  PUBLISHED
  CLOSED
}

Enum application_status {
  SUBMITTED
  SHORTLISTED
  REJECTED
  WITHDRAWN
}

Table employees [headercolor: #2F6DB5] {
  id            uuid          [pk]
  full_name     varchar(120)  [not null]
  email         varchar(160)  [not null, unique, note: 'Stored lower-case']
  password_hash varchar(100)  [not null, note: 'BCrypt hash, never returned by any endpoint']
  role          employee_role [not null, default: 'EMPLOYEE']
  active        boolean       [not null, default: true, note: 'Inactive accounts cannot log in']
  created_at    timestamptz   [not null]

  Note: 'People who log in. Seeded in Week 1, registration arrives in Week 2.'
}

Table skills [headercolor: #2F6DB5] {
  id          uuid        [pk]
  name        varchar(80) [not null, unique]
  description varchar(500)
  category    varchar(60) [not null]
  active      boolean     [not null, default: true, note: 'Inactive skills cannot be newly claimed or required']
  created_at  timestamptz [not null]

  Note: 'Skill catalogue. Deactivated, never deleted.'
}

Table skill_assessments [headercolor: #D9822B] {
  id               uuid              [pk]
  employee_id      uuid              [not null, note: 'Who claims the skill']
  skill_id         uuid              [not null]
  claimed_level    skill_level       [not null]
  evidence_url     varchar(500)      [note: 'External proof link, http or https']
  evidence_summary varchar(1000)     [not null]
  status           assessment_status [not null, default: 'PENDING']
  reviewer_id      uuid              [note: 'Null while PENDING, must differ from employee_id']
  reviewed_at      timestamptz       [note: 'Set together with reviewer_id']
  review_comment   varchar(500)
  created_at       timestamptz       [not null]
  updated_at       timestamptz       [not null]

  indexes {
    (employee_id, skill_id) [unique, name: 'uq_assessment_employee_skill']
    status
    skill_id
  }

  Note: 'MAIN ENTITY (Week 1 CRUD). One claim per employee and skill.'
}

Table opportunities [headercolor: #7A4FB5] {
  id           uuid               [pk]
  title        varchar(150)       [not null]
  description  varchar(2000)      [not null]
  created_by   uuid               [not null]
  status       opportunity_status [not null, default: 'DRAFT']
  closing_date date
  created_at   timestamptz        [not null]
  updated_at   timestamptz        [not null]

  indexes {
    status
    created_by
  }

  Note: 'Internal role or stretch assignment. DRAFT then PUBLISHED then CLOSED.'
}

Table opportunity_requirements [headercolor: #7A4FB5] {
  id             uuid        [pk]
  opportunity_id uuid        [not null]
  skill_id       uuid        [not null]
  minimum_level  skill_level [not null]

  indexes {
    (opportunity_id, skill_id) [unique, name: 'uq_requirement_opportunity_skill']
  }

  Note: 'Join table, Opportunity many-to-many Skill.'
}

Table applications [headercolor: #3A8F5C] {
  id             uuid               [pk]
  employee_id    uuid               [not null]
  opportunity_id uuid               [not null]
  status         application_status [not null, default: 'SUBMITTED']
  created_at     timestamptz        [not null]
  updated_at     timestamptz        [not null]

  indexes {
    (employee_id, opportunity_id) [unique, name: 'uq_application_employee_opportunity']
    opportunity_id
  }

  Note: 'Join table, Employee many-to-many Opportunity.'
}

TableGroup identity_and_catalogue {
  employees
  skills
}

TableGroup skill_verification {
  skill_assessments
}

TableGroup internal_mobility {
  opportunities
  opportunity_requirements
  applications
}

Ref: skill_assessments.employee_id > employees.id [delete: restrict]
Ref: skill_assessments.reviewer_id > employees.id [delete: restrict]
Ref: skill_assessments.skill_id > skills.id [delete: restrict]
Ref: opportunities.created_by > employees.id [delete: restrict]
Ref: opportunity_requirements.opportunity_id > opportunities.id [delete: cascade]
Ref: opportunity_requirements.skill_id > skills.id [delete: restrict]
Ref: applications.employee_id > employees.id [delete: restrict]
Ref: applications.opportunity_id > opportunities.id [delete: restrict]
```