# Week 3 milestones and traceability

Source of truth: "BuildLab Stage 1 Work Guide - Backend Development", Week 3 (Quality and delivery).
Rule: a milestone is closed only when its verification output exists. Nothing is marked done on a belief.

## What the guide requires in Week 3
| Guide item | Task |
|---|---|
| 3.1 Unit tests for key logic | Testing |
| 3.1 Integration tests for key endpoints, including auth and failure cases (400, 401, 403, 404) | Testing |
| 3.1 A separate test database or an in-memory database; tests run with one command, noted in the README | Testing |
| 3.1 Coverage note: what is tested, what is not, why | Testing |
| 3.2 Every endpoint documented (OpenAPI, Postman or a README table) with auth and examples | Documentation, security and deploy |
| 3.2 Security basics: secrets in environment variables, input sanitisation, CORS set deliberately, basic rate limiting on auth routes | same |
| 3.2 Performance basics: pagination on list endpoints, indexes on filtered fields | same |
| 3.2 Deployed, or a Docker or step-by-step local setup; setup guide; security and performance notes | same |
| Final package: README or case study, 5-minute walkthrough video, technical reflection, declaration | Final package |
| Deliverable: tag `week-3`, update note | How to submit |

## Milestones
| ID | Milestone | Acceptance criteria (as built) | Verification | Status |
|---|---|---|---|---|
| W3-0 | Branch, tracker, feedback response | Work on branch `week-3-dev`; tags `week-1` and `week-2` untouched; feedback response states the real Week 1 scores | `git ls-remote --tags origin`; this file | Verified (branch pushed). Tag check output to be re-run before tagging |
| W3-1 | Test infrastructure | Tests run against a throwaway PostgreSQL 16 (Testcontainers) with Flyway migrations; never the development database | Log: JDBC URL on a random port, database `test`; 3 tests green | Verified |
| W3-2 | Auth, validation and access-control tests | 400, 401, 403, 404, 405 and 409 cases pass in the standard error format | 18 tests green | Verified |
| W3-3 | Unit tests for business rules | Match calculation (rounding, highest level, verified only), application transitions, review order, level order, request normalisation | 42 unit tests green | Verified |
| W3-4 | Coverage measured and noted | JaCoCo report; `docs/test-coverage.md` states numbers, gaps and reasons | Instruction coverage 40.1% with 60 tests, 76.1% with 74 tests; final figure in the note | In progress: final figure to be added |
| W3-5 | End-to-end journey and assessment rules | One test walks claim, verify, publish, match 0% then 100%, apply, shortlist, withdraw; 13 assessment-rule tests | 74 tests green | Verified |
| W3-6 | Pagination and indexes | `page` and `size` validated; stable order; `X-Total-Count`; migration V6; filters in the database | 84 tests green; `EXPLAIN ANALYZE` before and after on 50,000 rows (9.1 ms to 0.06 ms; 29.2 ms to 0.03 ms); `docs/performance-notes.md` | Verified |
| W3-7 | Security basics | Rate limit on register and login (429 with `Retry-After`); CORS closed by default and configurable; injection, markup and error-leak tests | 96 tests green; `docs/security-notes.md` | Verified |
| W3-8 | Endpoint documentation | `docs/api-spec.md` updated for paging, 429 and CORS; Postman collection with paging checks; `docs/openapi.yaml`; Swagger UI via springdoc (`/swagger-ui.html`), guarded by `OpenApiDocsTest` | Spec reviewed against the code | Verified |
| W3-9 | Setup guide and README | A reviewer can run the app and the tests from a clean clone using only the guide | Clean-clone run of the guide, smoke test and `mvnw test` | Pending: clean-clone run |
| W3-10 | Reflection, declaration, AI log | Written in my own words and complete | The three files | Pending |
| W3-11 | Walkthrough video | 5 minutes: live requests and one technical decision | Video link | Pending |
| W3-12 | Tag `week-3`, update note, form | Tag pushed; confirmation received | `git ls-remote --tags origin`; email | Pending |