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
| W1-13 | Git tag week-1 pushed before the deadline | M | Tag visible on GitHub | `git ls-remote --tags origin`: 361dbf3fcb0ee2b0602ac32d4d4986da3afbda80 refs/tags/week-1 | [x] |
| W1-14 | 5-minute presentation: ERD, spec, a live request per endpoint | M | Timed rehearsal | Rehearsal time | [ ] |
| W1-15 | Weekly update note and form submitted; confirmation within 1 hour | M | Confirmation received | Confirmation email | [ ] |

## Week 2
| ID | Requirement | Type | Done when | Evidence | Status |
|---|---|---|---|---|---|
| W2-1 | List each piece of Week 1 feedback and what changed or why not | M | Every item answered | docs/feedback-response.md (states honestly that no feedback had arrived) | [x] |
| W2-2 | Issue log with 5 or more entries (bug, options, fix) | M | 5 or more entries | docs/issue-log.md, entries 9 to 20 from Week 2 | [x] |
| W2-3 | Sign-up and login with JWT or sessions | M | Both work | Register 201, login 200 and a token accepted on a protected call; smoke test and Postman collection | [x] |
| W2-4 | Passwords hashed with bcrypt or argon2; never stored or logged in plain text | M | Hash visible in the DB; logs checked | `length(password_hash)` = 60 and a `crypt()` match for the demo accounts; startup log search for "generated security password" printed nothing | [x] |
| W2-5 | Only logged-in users create or change data | M | No token gives 401 | Every `/api` endpoint except register and login answered 401 without a token (W2-1 checks, smoke test) | [x] |
| W2-6 | Ownership checks and a role | M | Wrong owner or role gives 403 | W2-3 to W2-8 scripts: employee 403 on review and create, other manager 403, ADMIN allowed | [x] |
| W2-7 | Relationships with endpoints to fetch related data | M | Related data endpoints match the spec | `/employees/{id}/skill-assessments`, opportunities with their requirements, applications list, `/opportunities/{id}/match`; W2-7 scripts | [x] |
| W2-8 | Validate every input | M | Bad input gives 400 everywhere | W2-8 section 2: 21 limit cases all 400, plus malformed JSON, unknown enums and malformed ids | [x] |
| W2-9 | One consistent error format and a global handler; no stack traces | M | Same shape for every error | W2-8 section 1: 415, 400, 406, 401, 405, 404 all in the one format; `ApiErrorController` for `/error` | [x] |
| W2-10 | Edge cases: duplicates, deleting a parent with children, invalid ids | M | Each handled deliberately | 409s for duplicates (email, skill name, requirement, pending claim, application); deleting a DRAFT removes its requirements; 400 and 404 for bad ids | [x] |
| W2-11 | Updated sample requests, tag week-2, update note | M | Tag pushed; confirmation received | git ls-remote, email | [ ] |

## Week 3
| ID | Requirement | Type | Done when | Evidence | Status |
|---|---|---|---|---|---|
| W3-1 | Unit tests for key logic | M | Business rules covered | 49 unit tests green: match calculation, application transitions, review order, level order, request normalisation, paging parameters, rate limiter (`.\mvnw.cmd test`, 97 tests, 0 failures) | [x] |
| W3-2 | Integration tests incl. auth and 400 / 401 / 403 / 404; separate test DB | M | Failure cases covered | 48 integration tests on a throwaway PostgreSQL 16 (Testcontainers): 400, 401, 403, 404, 405, 409 and 429 cases, a full journey test | [x] |
| W3-3 | Tests pass with one command, noted in the README | M | Green on a clean clone | `.\mvnw.cmd test` documented in README and setup guide. Clean-clone run still to do | [ ] |
| W3-4 | Coverage note: what is tested, what is not, why | M | Honest note | docs/test-coverage.md (JaCoCo: 40.1% at 60 tests, 76.1% at 74, 77% at 97 tests; branches 59%; gaps listed with evidence) | [x] |
| W3-5 | Every endpoint documented (OpenAPI, Postman or README table) with auth and examples | M | A stranger can call it | docs/api-spec.md (29 endpoints, conventions, error table, examples), docs/openapi.yaml (31 operations, hand-written, `$ref`s checked but not run through an official validator), Swagger UI from springdoc (97 tests green with it; `OpenApiDocsTest` checks `/v3/api-docs`; confirmed in the browser against the Docker build: Swagger UI loads, register and login work and return a token; an authorised `GET /api/skills` returns 200 and the same call without a token returns 401), and postman/skillbridge-api.postman_collection.json (43 requests, folder 7 for paging; run once on a database freshly reset with `docker compose down -v`, and every response matched the expected outcome in the Postman console log; Runner pass counts not captured) | [x] |
| W3-6 | Security basics: secrets in env, input sanitisation, CORS set deliberately, rate limiting on auth routes | M | Each item shown | docs/security-notes.md; tests for 429, CORS, injection-looking input, markup and error leaks | [x] |
| W3-7 | Performance basics: pagination on lists, indexes on filtered fields | M | Both shown | docs/performance-notes.md; migration V6; `EXPLAIN ANALYZE` before and after on 50,000 rows | [x] |
| W3-8 | Deployed, or a Docker or step-by-step local setup; setup guide | M | Works from a clean machine | docs/setup-guide.md. Clean-clone run still to do | [ ] |
| W3-9 | README or case study: problem, data model, architecture, decisions, limitations | M | Complete | README.md | [ ] |
| W3-10 | 5-minute walkthrough video: live requests and one technical decision | M | Recorded | Video link | [ ] |
| W3-11 | Technical reflection | M | Changes, risks, next steps | docs/technical-reflection.md | [ ] |
| W3-12 | Declaration: own work, libraries, boilerplates, material AI use | M | Honest and complete | docs/declaration.md (running record in docs/ai-usage-log.md) | [ ] |
| W3-13 | Tag week-3, exported OpenAPI or collection as backup, update note | M | Tag pushed; confirmation received | git ls-remote, email | [ ] |

## Final checklist (from the guide)
- [ ] Public repo with tags week-1, week-2, week-3
- [ ] Live API or a run guide that works from a clean machine
- [ ] Tests pass with one command
- [ ] API docs, setup guide, security and performance notes
- [ ] Walkthrough video, reflection and declaration
- [ ] No secrets, keys or real passwords in the repo or history

## Red-flag self-audit (run before every tag)
- [x] No secrets or real credentials in the repo or history (1 Nov 2026: `git ls-files .env` prints nothing and `git log --all -p -G"JWT_SECRET=[A-Za-z0-9+/]{20,}"` prints nothing; the pattern only catches a secret written as `JWT_SECRET=` followed by 20 or more base64 characters)
- [x] No plain-text passwords stored anywhere, including seed data (the seed holds a BCrypt hash of a documented dummy demo password; the plaintext appears only in the README, the Postman collection and the smoke script, as a dummy value)
- [x] Every input validated; no stack traces in any response (checked on the Week 1 requests and in W2-8)
- [x] Every implemented endpoint matches docs/api-spec.md
- [x] README steps run from a clean clone (23 Oct 2026: a fresh clone of the `week-2` branch passed the smoke test, ALL 34 CHECKS PASSED)
- [ ] Work is original, not a tutorial API with the names changed

## Known limitations at Week 3 (stated, not hidden)
- No hosted deployment, on purpose: the seeded demo accounts share a documented password. A Docker run guide and a local setup guide are provided.
- No HTTPS in the application, no token revocation or refresh, no account lockout, no dependency vulnerability scan (docs/security-notes.md).
- The rate limiter is in memory: per instance, reset on restart, fixed window.
- Offset pagination gets slower on very deep pages.
- Withdrawing a PENDING claim deletes it permanently; a withdrawn application cannot be re-submitted.
- Tests do not cover concurrency, the role and active-status endpoints in depth or some uncommon error handlers (docs/test-coverage.md).
- Full list with reasons: docs/issue-log.md.

## My own recommendations (not required by the guide)
- R1 UUID primary keys
- R2 Mockito, MockMvc and Testcontainers for tests (done in Week 3)
- R3 GitHub Actions CI
- R4 Dockerised application, beyond the database
- R5 PDF copies of the ERD, spec and notes in a Google Drive folder, linked from the README