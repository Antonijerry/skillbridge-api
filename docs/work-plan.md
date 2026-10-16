# Work plan: SkillBridge API

Owner: ANTHONY JERRY K.   Track: Backend Development   Repo: skillbridge-api
Main entity: SkillAssessment (Week 1 CRUD). Employee and Skill are seeded reference data.

## Week 1: Design and build (due Friday 16 October 2026)
Deliverables
- Idea one-pager, this work plan, public repo, starter README and .env.example
- ERD and API spec for the whole product, written and committed before any controller code
- SkillAssessment CRUD: POST, GET (list), GET (one), PUT, DELETE on /api/skill-assessments
- Input validation and status codes 201 / 200 / 204 / 400 / 404, plus 409 for a second PENDING claim
  (PENDING_CLAIM_EXISTS), a claim already VERIFIED at the same or a higher level (ALREADY_VERIFIED)
  and editing or withdrawing a non-PENDING claim (ASSESSMENT_NOT_EDITABLE)
- Assessment history: many assessments per employee and skill, at most one PENDING (partial unique index)
- Seeded dummy employees, skills and assessments (Flyway V3); sample requests in requests/*.json
- Tag week-1; 5-minute presentation on the weekend after the deadline
  Not in Week 1: login, roles, verify/reject, opportunities, applications, GET /api/skills (all specified, not built)

## Week 2: Auth and robustness
- Respond to Week 1 feedback; issue log with 5 or more entries
- Registration, login, BCrypt hashing, JWT; roles EMPLOYEE / MANAGER / ADMIN; 401 vs 403
- Ownership checks; employee comes from the token, never the request body
- Relationships in JPA (replace the Week 1 plain UUID columns), read-only GET /api/skills and related-data endpoints
- Verify/reject workflow (no self-verification), opportunities with required skills,
  match percentage, applications
- One error format, global handler, edge cases (duplicates, deleting parents, invalid ids)
- Tag week-2

## Week 3: Quality and delivery
- Unit and integration tests including 400 / 401 / 403 / 404, one-command run, honest coverage note
- OpenAPI docs, setup guide, security notes, performance notes (pagination, indexes, CORS, auth rate limit)
- Deployment, or a Docker run guide tested from a clean clone
- README case study, 5-minute walkthrough, technical reflection, declaration
- Tag week-3

## Top 3 risks
| Risk | Mitigation | Early warning sign |
|---|---|---|
| Scope creep in match scoring | v1 is only the percentage of requirements met by VERIFIED assessments at or above the minimum level | Any request for weighting or ranking |
| Security and ownership vulnerabilities (missed role check, manager verifying their own claim) | Method-level security, rules enforced in services, tests for every 401 and 403 path | A protected endpoint with no negative test |
| Environment drift between local and deployed setups | Schema only through Flyway; run steps re-tested from a clean clone each week | "Works on my machine" |

## If I fall behind (cut order)
1. Application status workflow
2. Pagination polish and extra filters
3. Deployment (fall back to a tested Docker run guide)
   Never cut: ownership checks, 401/403 tests, validation, the error format.

## Definition of done (every week)
- App runs from the README steps on a clean clone
- No secrets in the repo or history
- Docs match the actual behaviour
- Weekly tag pushed before the deadline, update note submitted, confirmation received
