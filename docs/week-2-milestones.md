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
| ID | Milestone | Guide item | Acceptance criteria | Verification | Status |
|---|---|---|---|---|---|
| W2-0 | Branch, feedback response, issue log | Start of week | Branch `week-2` exists; issue log has 5 or more real entries; feedback response states honestly that no feedback has arrived | `git log`, the two files | In progress: files written, not yet committed |
| W2-1 | Security wall | 2.1 logged-in only; 401 | App starts; every endpoint except register and login returns 401 in the project error format with no token and with a garbage token | curl output, startup log | Files prepared, not built |
| W2-2 | Register and login | 2.1 sign-up, login, hashing | Register returns 201 with no hash in the body; duplicate e-mail 409; login returns a token; wrong password, unknown e-mail and inactive account give the same 401; hash visible in the DB; password absent from logs | curl output, psql output, log search | Files prepared, not built |
| W2-3 | Employee from the token; ownership | 2.1 ownership, 403 | `employeeId` removed from the create request; PUT and DELETE on someone else's assessment give 403; list shows only your own rows for an EMPLOYEE | curl output with two accounts | Not started |
| W2-4 | Roles: verify and reject | 2.1 role | Only MANAGER and ADMIN can verify or reject; reviewing your own claim is refused; EMPLOYEE gets 403 | curl output per role | Not started |
| W2-5 | Relationships and related-data endpoints | 2.2 relationships | Plain UUID columns replaced by JPA relationships; `GET /api/skills`; related-data endpoints match the spec | curl output, schema validation passes | Not started |
| W2-6 | Opportunities and requirements | 2.2 relationships | V4 migration applied; DRAFT, PUBLISHED, CLOSED lifecycle; one requirement per skill per opportunity; deleting a parent with children refused | curl output, Flyway log | Not started |
| W2-7 | Match percentage and applications | 2.2 relationships | Match uses only VERIFIED assessments at or above the minimum; one application per employee and opportunity | curl output | Not started (first to cut) |
| W2-8 | Validation and error pass | 2.2 validate, error format, edge cases | Every input validated; duplicates, parent with children and invalid ids each demonstrated; no stack trace anywhere | curl output per case | Not started |
| W2-9 | Delivery | Submission | Postman collection with a login step; clean-clone test; tag `week-2`; update note; confirmation | `git ls-remote`, confirmation email | Not started |

## Cut line
If W2-7 is not done by mid-week, applications become "documented, not built". Never cut ownership checks, 401 and 403 behaviour, validation or the error format.

## Known limitations to record in the issue log as they appear
- A deactivated account's existing token stays valid until it expires (BR-7 only blocks login). Option: check `active` on every request.
- Login timing reveals whether an e-mail exists, because BCrypt only runs for real accounts.
