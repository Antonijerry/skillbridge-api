# SkillBridge API
Track: Backend Development   Stack: Java 21, Spring Boot 4.0.8, Spring Data JPA, PostgreSQL, Flyway, Spring Security + JWT (from Week 2), Docker

Target user: An employee in a mid-sized organisation seeking internal mobility or a stretch role.
Internal hiring managers and talent leads take part as the people who verify that employee's skills.

Problem: Employees struggle to find transparent pathways for growth, while hiring managers
cannot identify qualified internal talent because they rely on unverified, self-reported skills
and fragmented records.

Entities: Employee, Skill, SkillAssessment, Opportunity, OpportunityRequirement, Application
Relationships: an Employee submits many SkillAssessments, each for one Skill; an Opportunity
requires many Skills through OpportunityRequirement (with a minimum level); an Employee applies
to many Opportunities through Application.
Week 1 main entity: SkillAssessment (full CRUD). Employee and Skill are seeded reference data.

Core flow: POST /api/auth/login -> POST /api/skill-assessments -> PATCH /api/skill-assessments/{id}/verify
-> GET /api/opportunities -> GET /api/opportunities/{id}/match -> POST /api/applications
Week 1 builds the skill-assessment part of this flow (POST, GET list, GET one, PUT, DELETE on
/api/skill-assessments) with seeded employees and skills. Login, verify, opportunities, match and
applications are specified and arrive in Week 2.

In scope:
- Skill assessments with an external proof URL, history per employee and skill (at most one PENDING claim at a time) and a manager review workflow (PENDING / VERIFIED / REJECTED)
- Internal opportunities with required skills and minimum levels (DRAFT / PUBLISHED / CLOSED)
- Match percentage per opportunity, calculated only from VERIFIED assessments, and applications to published opportunities
- Stateless JWT authentication, BCrypt password hashing, roles EMPLOYEE / MANAGER / ADMIN and ownership checks (Week 2)
- Versioned database schema and seed data through Flyway

Out of scope:
- File or certificate uploads (links only)
- Email, SMS or push notifications
- Frontend or dashboards
- AI-based matching, and a manager-to-report hierarchy
- Reopening a closed opportunity

Top 3 risks:
1. Scope creep in match scoring.
   Mitigation: v1 is only the percentage of requirements met by verified skills at or above the minimum level.
2. Security and ownership vulnerabilities, for example a missed role check or a manager verifying their own claim.
   Mitigation: method-level security, rules enforced in services, and tests for the 401 and 403 failure paths.
3. Environment drift between local and deployed setups.
   Mitigation: schema only through Flyway migrations, and a containerised run documented and tested from a clean clone.

Week 2 plan (Auth and robustness):
- Respond to Week 1 feedback (docs/feedback-response.md) and keep an issue log with 5 or more entries (docs/issue-log.md)
- Registration and login with BCrypt hashing and JWT; roles EMPLOYEE / MANAGER / ADMIN; 401 vs 403; the employee comes from the token, never the request body
- JPA relationships replacing the plain UUID columns, plus related-data endpoints and read-only GET /api/skills
- Verify/reject workflow (a reviewer can never review their own claim), opportunities with required skills, match percentage, applications
- Validation on every input, one error format, a global handler, and edge cases (duplicates, deleting a parent with children, invalid ids)

Week 3 plan (Quality and delivery):
- Unit and integration tests including 400 / 401 / 403 / 404, a separate test database, one-command run and an honest coverage note
- OpenAPI/Swagger docs, Postman collection, setup guide, security notes (secrets in env, CORS, rate limiting on auth routes) and performance notes (pagination, indexes)
- Deployment, or a Docker run guide tested from a clean clone
- Final package: README case study, 5-minute walkthrough video, technical reflection and declaration (including AI use)

Data: dummy data only. No real personal data or passwords.