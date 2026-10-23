# Week 2 milestones and traceability

Source of truth: "BuildLab Stage 1 Work Guide - Backend Development", Week 2 (Auth and robustness).
Rule: a milestone is closed only when its verification output exists. Nothing is marked done on a belief.

## What the guide requires in Week 2
| Guide item | Where in the guide |
|---|---|
| Respond to Week 1 feedback: list each item and what changed or why not | Week 2, start of week |
| Issue log with 5 or more entries: bugs found, options considered, how fixed | Week 2, start of week |
| 2.1 Sign-up and login with JWT or sessions | Task 2.1 |
| 2.1 Hash passwords with bcrypt or argon2; never store or log plain passwords | Task 2.1 |
| 2.1 Only logged-in users can create or change data | Task 2.1 |
| 2.1 Ownership checks; a simple role if the idea needs it | Task 2.1 |
| 2.1 Return 401 when not logged in and 403 when not allowed | Task 2.1 |
| 2.2 Relationships from the ERD, with endpoints to fetch related data | Task 2.2 |
| 2.2 Validate every input with a schema or validation library | Task 2.2 |
| 2.2 One consistent error format and a global handler; no stack traces | Task 2.2 |
| 2.2 Edge cases: duplicate records, deleting a parent with children, invalid ids | Task 2.2 |
| Deliverable: updated API, tagged week-2, updated sample requests, update note | Task 2.2 and How to submit |

## Milestones
| ID | Milestone | Guide item | Acceptance criteria (as built) | Verification | Status |
|---|---|---|---|---|---|
| W2-0 | Branch, feedback response, issue log | Start of week | Branch `week-2` exists; issue log has 5 or more real entries (now 20 in total, entries 9 to 20 from Week 2); feedback response states honestly that no feedback has arrived | `git log`, the two files | Verified: committed on `week-2` |
| W2-1 | Security wall | 2.1 logged-in only; 401 | App starts; every endpoint except register and login returns 401 in the project error format with no token and with a garbage token | curl output, startup log (no generated password) | Verified |
| W2-2 | Register and login | 2.1 sign-up, login, hashing | Register returns 201 with no hash in the body; duplicate e-mail 409; login returns a token; wrong password, unknown e-mail and inactive account give the same 401; hash visible in the DB; password absent from logs | curl output, psql output, log search | Verified |
| W2-3 | Employee from the token; ownership | 2.1 ownership, 403 | `employeeId` removed from the create request (a client-sent id is ignored); PUT and DELETE on someone else's assessment give 403; list shows only your own rows for an EMPLOYEE; a deactivated account's token gives 401 at once | `w2-3` check output with several accounts | Verified |
| W2-4 | Roles: verify and reject | 2.1 role | Only MANAGER and ADMIN can verify or reject; reviewing your own claim is refused; EMPLOYEE gets 403; a reject needs a comment | `w2-4-checks.ps1` output per role | Verified |
| W2-5 | Skills catalogue and related-data endpoints | 2.2 relationships | `GET /api/skills` and the skill endpoints; assessments carry `skillName`; `GET /api/employees/{id}/skill-assessments` matches the spec. **Deviation:** the plain UUID columns were deliberately kept (design decision 20) instead of becoming JPA relationships; the foreign keys exist in the database and the related data is returned by the endpoints | `w2-5-checks.ps1` output, schema validation passes | Verified, with the stated deviation |
| W2-6 | Opportunities and requirements | 2.2 relationships | V4 migration applied; DRAFT, PUBLISHED, CLOSED lifecycle; one requirement per skill per opportunity; publishing needs a requirement and a future closing date; a published opportunity is frozen; deleting a DRAFT removes its requirements, deleting a non-DRAFT is refused (409) | `w2-6-checks.ps1` output, Flyway log, `requirements_left_for_C = 0` | Verified |
| W2-7 | Match percentage and applications | 2.2 relationships | Match uses only VERIFIED assessments at or above the minimum, rounded down (2 of 3 gave 66); one application per employee and opportunity; own opportunity refused; shortlist, reject and withdraw with allowed transitions only; listing scoped by role | `w2-7-checks.ps1` and `w2-7-recount.ps1` output | Verified |
| W2-8 | Validation and error pass | 2.2 validate, error format, edge cases | Every input validated (21 limit cases all 400); duplicates, parent with children and invalid ids each demonstrated; 415, 406 and malformed bodies in the one error format; `/error` returns the project format; admin role and deactivation endpoints; no stack trace anywhere | `w2-8-checks.ps1` output, `/error` probe | Verified; one accepted limitation (invalid percent-encoding returns the server's HTML 400) |
| W2-9a | Demo logins, smoke test, Postman | Submission | V5 demo passwords; smoke test `ALL 34 CHECKS PASSED`; Postman collection with a login step, 35 of 35 checks green | `w2-9-smoke.ps1` output, Postman run | Verified |
| W2-9b | Docs, clean-clone test, tag, update note | Submission | README, API spec, issue log and compliance checklist updated; clean clone of `week-2` passes the smoke test; tag `week-2` pushed; update note written; confirmation received | `git ls-remote`, clean-clone output, confirmation email | In progress: clean-clone test, tag and confirmation outstanding |

## Cut line
W2-7 was built, so nothing was cut. The rule stands for future work: never cut ownership checks, 401 and 403 behaviour, validation or the error format.

## Limitations recorded in Week 2 (details in docs/issue-log.md)
- A deactivated account's token used to stay valid until it expired. **Fixed:** the account is re-read on every request (issue 13).
- Login timing revealed whether an e-mail exists. **Narrowed:** login compares against a dummy hash when the e-mail is unknown, so the timing is similar (it is not claimed to be perfectly equal).
- Open, deliberately deferred: no pagination, last write wins on simultaneous reviews, no re-apply after withdrawing, the demo password seed is for demos only, invalid percent-encoding returns the server's plain HTML 400.