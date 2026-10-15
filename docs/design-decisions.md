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