# Response to Week 1 feedback

## Status
The Week 1 assessment arrived after the Week 2 submission. It contains **scores only: no written comments**. This response is therefore based on the scores, and no comment is invented. If written comments arrive, each one is added to the table below.

## Week 1 scores
Mentor weight 40%, peer weight 50% (the AI score was not active).

| Criterion | Mentor | Peer |
|---|---|---|
| Data model, API and business-rule design | 4/5 | 4/5 |
| Validation, authentication and security approach | 3/5 | 4/5 |
| Reliability, testing and operational readiness | 4/5 | 4/5 |
| Evidence gathering and synthesis | 4/5 | 2/5 |
| Assumptions, trade-offs and decision rationale | 3/5 | 4/5 |
| Completeness, organisation and reliability | 4/5 | 4/5 |
| Testing, review and iteration | 3/5 | 4/5 |
| Documentation and walkthrough | 3/5 | 4/5 |
| Learning, limitations and next-step judgement | 4/5 | 5/5 |

## What I did about the lower scores (Week 3)
| Lower score | What I did | Evidence |
|---|---|---|
| Validation, authentication and security approach (mentor 3) | Wrote down the security approach and tested it: 401, 403 and 429 cases, deliberate CORS, injection-looking input, markup in text, no internals in error bodies. Documented what is not protected | `docs/security-notes.md`; `AuthFailureTest`, `AccessControlTest`, `SecurityBasicsTest`, `CorsTest`, `RateLimitIntegrationTest` |
| Assumptions, trade-offs and decision rationale (mentor 3) | The README now has a table of key decisions with the alternative I rejected and why. Measured one decision (indexes) and recorded where the result surprised me | `README.md` (Key decisions); `docs/performance-notes.md`; issue log entries 21 to 28 |
| Testing, review and iteration (mentor 3) | Added an automated suite on a real PostgreSQL (97 tests), measured coverage and fixed the largest gap (the assessment workflow went from 3% to 77.7%) | `docs/test-coverage.md`; issue log entry 23 |
| Documentation and walkthrough (mentor 3) | Setup guide, updated API spec, case-study README and a recorded walkthrough | `docs/setup-guide.md`; `docs/api-spec.md`; `README.md`; the video |
| Evidence gathering and synthesis (peer 2) | Decisions in the notes now point to measured evidence (query plans, test output, coverage numbers) and list their references | `docs/performance-notes.md`; `docs/security-notes.md` (References) |

## Changes made in Week 2 regardless of feedback
| Change | Outcome | Evidence |
|---|---|---|
| Take the employee from the login token and remove `employeeId` from the request body | **Done (W2-3).** The employee is always the logged-in user; an `employeeId` sent by a client is ignored | W2-3 checks; issue log entry 13; `docs/api-spec.md` endpoint 1 |
| Replace plain UUID columns with JPA relationships | **Deliberately not done.** The foreign keys exist in the database and the related data is returned by the endpoints. In the code, features reference each other by id and only an opportunity owns its requirements through JPA. This keeps each feature independent and avoids loading whole object graphs (design decision 20) | `docs/design-decisions.md` row 20; `docs/week-2-milestones.md` W2-5 |
| Add `GET /api/skills` | **Done (W2-5)** | `docs/api-spec.md` endpoints 12 to 14; smoke test |