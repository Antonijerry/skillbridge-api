# Compliance checklist (BuildLab Stage 1)

Source of truth: "BuildLab Stage 1 Work Guide - Backend Development".
Tick a box only when there is evidence (a commit, a link or command output), never on a belief.
Type: M = mandatory in the guide, R = my own engineering recommendation.

## Week 1
| ID | Requirement | Type | Done when | Evidence | Status |
|---|---|---|---|---|---|
| W1-1 | Idea one-pager in the guide's template | M | All template fields filled | docs/idea-one-pager.md | [x] |
| W1-2 | One-page work plan with top 3 risks | M | Per-week deliverables and 3 risks | docs/work-plan.md | [x] |
| W1-3 | Public repo, starter README (idea, stack, how to run), .env.example | M | Opens in a private window; no real values | https://github.com/Antonijerry/skillbridge-api (public, checked in a private window) | [x] |
| W1-4 | At least 3 entities with key fields and types | M | Six entities documented | docs/erd.md | [x] |
| W1-5 | ERD showing one-to-many, many-to-many and keys | M | Both join entities drawn | docs/erd.png | [x] |
| W1-6 | API spec written before coding: method, path, purpose, request, response, status codes | M | Every endpoint complete; spec commit precedes controller commits | docs/api-spec.md; spec commit 74e5088 precedes controller commit 1b28807 | [x] |
| W1-7 | Design decisions noted | M | Reasons and alternatives recorded | docs/design-decisions.md | [x] |
| W1-8 | Project, DB connection and migrations | M | App starts; Flyway applies migrations | Clean-clone log: V1, V2, V3 applied, "Started SkillbridgeApiApplication" | [x] |
| W1-9 | Create, list, get, update, delete for the main entity (SkillAssessment) | M | Each endpoint matches the spec | 16 sample requests run against the running API, each status as specified | [x] |
| W1-10 | Validation; 201 on create, 200 on success, 400 on bad input, 404 when not found | M | One request per case | 201, 200, 204, 400 (x4), 404 (x2), 409 (x4) shown | [x] |
| W1-11 | Seed dummy data | M | Fresh DB has rows | Clean clone returned the 2 seeded assessments from a fresh database | [x] |
| W1-12 | Sample requests (curl, Postman or .http) | M | A stranger can run them | requests/*.json with curl.exe commands in the README | [x] |
| W1-13 | Git tag week-1 pushed before the deadline | M | Tag visible on GitHub | git ls-remote --tags | [ ] |
| W1-14 | 5-minute presentation: ERD, spec, a live request per endpoint | M | Timed rehearsal | Rehearsal time | [ ] |
| W1-15 | Weekly update note and form submitted; confirmation within 1 hour | M | Confirmation received | Confirmation email | [ ] |

## Week 2
| ID | Requirement | Type | Done when | Evidence | Status |
|---|---|---|---|---|---|
| W2-1 | List each piece of Week 1 feedback and what changed or why not | M | Every item answered | docs/feedback-response.md | [ ] |
| W2-2 | Issue log with 5 or more entries (bug, options, fix) | M | 5 or more entries | docs/issue-log.md | [ ] |
| W2-3 | Sign-up and login with JWT or sessions | M | Both work | Request output | [ ] |
| W2-4 | Passwords hashed with bcrypt or argon2; never stored or logged in plain text | M | Hash visible in the DB; logs checked | DB query, log search | [ ] |
| W2-5 | Only logged-in users create or change data | M | No token gives 401 | Request output | [ ] |
| W2-6 | Ownership checks and a role | M | Wrong owner or role gives 403 | Request output | [ ] |
| W2-7 | Relationships with endpoints to fetch related data | M | Related data endpoints match the spec | Request output | [ ] |
| W2-8 | Validate every input | M | Bad input gives 400 everywhere | Request output | [ ] |
| W2-9 | One consistent error format and a global handler; no stack traces | M | Same shape for every error | Request output | [ ] |
| W2-10 | Edge cases: duplicates, deleting a parent with children, invalid ids | M | Each handled deliberately | Request output | [ ] |
| W2-11 | Updated sample requests, tag week-2, update note | M | Tag pushed; confirmation received | git ls-remote, email | [ ] |

## Week 3
| ID | Requirement | Type | Done when | Evidence | Status |
|---|---|---|---|---|---|
| W3-1 | Unit tests for key logic | M | Business rules covered | Test output | [ ] |
| W3-2 | Integration tests incl. auth and 400 / 401 / 403 / 404; separate test DB | M | Failure cases covered | Test output | [ ] |
| W3-3 | Tests pass with one command, noted in the README | M | Green on a clean clone | Terminal output | [ ] |
| W3-4 | Coverage note: what is tested, what is not, why | M | Honest note | docs/test-coverage.md | [ ] |
| W3-5 | Every endpoint documented (OpenAPI, Postman or README table) with auth and examples | M | A stranger can call it | Swagger UI or collection | [ ] |
| W3-6 | Security basics: secrets in env, input sanitisation, CORS set deliberately, rate limiting on auth routes | M | Each item shown | docs/security-notes.md | [ ] |
| W3-7 | Performance basics: pagination on lists, indexes on filtered fields | M | Both shown | docs/performance-notes.md | [ ] |
| W3-8 | Deployed, or a Docker or step-by-step local setup; setup guide | M | Works from a clean machine | docs/setup-guide.md | [ ] |
| W3-9 | README or case study: problem, data model, architecture, decisions, limitations | M | Complete | README.md | [ ] |
| W3-10 | 5-minute walkthrough video: live requests and one technical decision | M | Recorded | Video link | [ ] |
| W3-11 | Technical reflection | M | Changes, risks, next steps | docs/technical-reflection.md | [ ] |
| W3-12 | Declaration: own work, libraries, boilerplates, material AI use | M | Honest and complete | docs/declaration.md | [ ] |
| W3-13 | Tag week-3, exported OpenAPI or collection as backup, update note | M | Tag pushed; confirmation received | git ls-remote, email | [ ] |

## Final checklist (from the guide)
- [ ] Public repo with tags week-1, week-2, week-3
- [ ] Live API or a run guide that works from a clean machine
- [ ] Tests pass with one command
- [ ] API docs, setup guide, security and performance notes
- [ ] Walkthrough video, reflection and declaration
- [ ] No secrets, keys or real passwords in the repo or history

## Red-flag self-audit (run before every tag)
- [ ] No secrets or real credentials in the repo or history (`git log -p` search; only .env.example committed)
- [ ] No plain-text passwords anywhere, including seed data
- [x] Every input validated; no stack traces in any response (checked on the 16 Week 1 requests)
- [x] Every implemented endpoint matches docs/api-spec.md
- [x] README steps run from a clean clone (verified on a fresh clone and a fresh database)
- [ ] Work is original, not a tutorial API with the names changed

## Known limitations at Week 1 (stated, not hidden)
- No authentication: employeeId is sent in the request body until Week 2.
- Assessments reference employees and skills by plain UUID columns; JPA relationships arrive in Week 2.
- GET /api/skills is specified for Week 2 and not built.
- Withdrawing a PENDING claim deletes it permanently.
- No hosted deployment; a Docker run guide is provided.

## My own recommendations (not required by the guide)
- R1 UUID primary keys
- R2 Mockito and MockMvc for tests
- R3 GitHub Actions CI
- R4 Dockerised application, beyond the database
- R5 PDF copies of the ERD, spec and notes in a Google Drive folder, linked from the README
