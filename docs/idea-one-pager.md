# SkillBridge API
Track: Backend Development   Stack: Java 21, Spring Boot 4.0.8, Spring Data JPA, PostgreSQL, Flyway, Spring Security + JWT, Docker

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

In scope:
- Skill assessments with an external proof URL and a manager review workflow (PENDING / VERIFIED / REJECTED)
- Internal opportunities with required skills and minimum levels (DRAFT / PUBLISHED / CLOSED)
- Match percentage per opportunity, calculated only from VERIFIED assessments
- Applications to published opportunities
- Stateless JWT authentication, BCrypt password hashing, roles EMPLOYEE / MANAGER / ADMIN and ownership checks
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

Data: dummy data only. No real personal data or passwords.