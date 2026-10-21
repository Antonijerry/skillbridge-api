# Design decisions

| # | Decision | Why | Alternative not chosen |
|---|---|---|---|
| 1 | PostgreSQL (relational) | The data is strongly relational and the rules (uniqueness, state) need constraints | MongoDB: no foreign keys or unique-pair guarantees without extra code |
| 2 | UUID primary keys | Ids cannot be enumerated; safe to expose | Sequential ids: simpler, but guessable |
| 3 | Many assessments per (employee, skill) over time; at most one PENDING, enforced by a partial unique index | Rejected claims can be resubmitted and a verified level can be upgraded, with no self-promotion: the old VERIFIED row keeps counting until the new one is reviewed | One row per pair: simpler, but dead-ends rejected employees and forces edits to verified rows |
| 4 | Review fields live on skill_assessments | One reviewer per claim in v1 | Separate reviews table: needed only for multi-reviewer or audit history |
| 5 | review_comment column added | A rejection without a reason is not actionable for the employee | Omit it: one column fewer, but a worse workflow |
| 6 | Join entities with their own columns (minimum_level, status) | Both many-to-many links carry data | Plain link tables cannot hold levels or application state |
| 7 | Status and level as VARCHAR with CHECK, mapped to Java enums as strings | Readable in the database; adding a value is a simple migration | Native Postgres enums: harder to migrate; ordinals: unreadable and fragile |
| 8 | Skills and employees are deactivated, not deleted | History must survive; foreign keys use RESTRICT | Cascade delete: silently destroys verified evidence |
| 9 | Match percentage informs and does not block applying | Stretch roles mean employees may not meet every requirement | Hard gate: simpler, but defeats the stretch-role goal |
| 10 | Ownership failures return 403 | Matches the guide's 401 and 403 requirement; UUIDs make existence hard to guess | 404 for hidden resources: leaks less, but contradicts the guide's wording |
| 12 | Stateless JWT (HS256, 60 minutes, secret from `JWT_SECRET`), no refresh tokens | Simple, no server session store, fits a stateless API; the guide accepts JWT or sessions | Server sessions: need shared storage when scaled. Refresh tokens: more endpoints and revocation logic, out of scope for three weeks |
| 13 | One `INVALID_CREDENTIALS` response for unknown email, wrong password and inactive account | The API must not reveal which emails exist | Separate messages: friendlier, but enables account enumeration |
| 14 | Registration always creates EMPLOYEE; roles change only through an ADMIN endpoint | Prevents self-promotion to MANAGER or ADMIN | A role field on register: trivially exploitable |
| 15 | Employee identity comes only from the token; a client-sent `employeeId` is ignored | A user can never act for someone else (BR-1) | Trusting the body: the Week 1 stand-in, removed now |
| 16 | An employee requesting a record they may not see gets 403, not 404 | Consistent with decision 10 and the guide's 401/403 wording | 404 for hidden records: leaks less, but is inconsistent with decision 10 |
| 17 | Match percentage is computed on request from VERIFIED assessments, never stored | A stored value would go stale when an assessment is verified or an opportunity changes | A stored column: faster reads, needs invalidation |
| 18 | Pagination stays in Week 3; the application status workflow is the first cut if time runs short | Matches the Week 1 spec and the work plan's cut order | Pagination now: more work in the busiest week |
| 19 | Demo passwords for the seeded accounts, hash only in migration V5, plaintext only in the README, for local use | Mentors need a MANAGER and an ADMIN to test the review flow | An env-based admin bootstrap: more realistic, more code. Revisit before any public deployment in Week 3 |