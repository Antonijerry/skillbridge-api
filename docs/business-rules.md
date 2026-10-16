# Business rules and permissions

Companion to docs/api-spec.md (the contract) and docs/erd.md (the data model).
Endpoint paths match the API spec; Week 2 paths are provisional until their bodies are specified.

## Roles
- EMPLOYEE: the default. Registration can only ever create this role.
- MANAGER: everything an employee can do, plus reviewing assessments and managing opportunities.
- ADMIN: everything a manager can do, plus managing skills and employee accounts.
  Each role includes the one below it.

## Rules
Week = when the rule starts to be enforced. Error code = what the API returns when it is broken
(see the error table in docs/api-spec.md; Week 2 codes are named when those endpoints are specified).

| ID | Rule | Enforced in | Error | Week |
|---|---|---|---|---|
| BR-1 | An employee submits assessments only for themselves. The employee comes from the login token, never the request body (Week 1 uses the body as a temporary stand-in) | Service | 403 FORBIDDEN | 2 |
| BR-2 | History is kept: an employee can have many assessments per skill over time, but at most one PENDING per employee and skill. A second PENDING claim is refused | Database partial unique index `uq_assessment_one_pending`, plus a friendly check in the service | 409 PENDING_CLAIM_EXISTS | 1 |
| BR-3 | A new assessment starts as PENDING. Edit and withdraw are allowed only while PENDING | Service | 409 ASSESSMENT_NOT_EDITABLE | 1 |
| BR-4 | Only a MANAGER or ADMIN can verify or reject, and never their own claim | Security, service and a database CHECK | 403 FORBIDDEN | 2 |
| BR-5 | A reviewed assessment records reviewer and reviewedAt. VERIFIED and REJECTED are final and immutable. A rejected claim is resubmitted with a new claim (see BR-17), never edited | Service and database CHECK | 409 ASSESSMENT_NOT_EDITABLE | 2 |
| BR-6 | Inactive skills cannot be newly claimed or required | Service | 409 SKILL_INACTIVE | 1 |
| BR-7 | Inactive employees cannot log in | Authentication | 401 | 2 |
| BR-8 | Opportunity lifecycle: DRAFT -> PUBLISHED -> CLOSED, with no going back | Service | 409 | 2 |
| BR-9 | Publishing needs at least one requirement and a closing date in the future | Service | 409 or 400 | 2 |
| BR-10 | Requirements can be added or removed only while DRAFT; one requirement per skill per opportunity | Service and database unique constraint | 409 | 2 |
| BR-11 | Eligibility: for every requirement, the employee has at least one VERIFIED assessment for that skill at or above the minimum level. A later REJECTED or PENDING claim never revokes an earlier VERIFIED one | Service query | n/a | 2 |
| BR-12 | Match percentage = requirements met / total requirements x 100, rounded down. It informs the manager and does not block applying | Service | n/a | 2 |
| BR-13 | Applying needs a PUBLISHED opportunity whose closing date has not passed. One application per employee and opportunity. You cannot apply to your own opportunity | Service and database unique constraint | 409 | 2 |
| BR-14 | A parent with children is never silently deleted. Skills and employees are deactivated instead; only DRAFT opportunities and PENDING assessments can be deleted | Foreign keys with RESTRICT, plus service | 409 | 1 (assessments), 2 (rest) |
| BR-15 | Not logged in returns 401; logged in but not allowed returns 403. IDs are UUIDs, but ownership checks are the real protection | Security and service | 401 / 403 | 2 |
| BR-16 | A malformed id returns 400; a well-formed unknown id returns 404 | Controller and service | 400 BAD_REQUEST / 404 NOT_FOUND | 1 |
| BR-17 | A new claim (or a level change on a PENDING claim) is refused if the employee already has a VERIFIED assessment for that skill at the same or a higher level. A higher level is allowed and starts a new PENDING claim while the old VERIFIED row keeps counting. Level order: BEGINNER < INTERMEDIATE < ADVANCED < EXPERT | Service (compares levels, so a database index cannot express it) | 409 ALREADY_VERIFIED | 1 |

## What exists in Week 1 (honest scope)
Week 1 has no authentication. These rules are enforced in the Week 1 build: BR-2, BR-3, BR-6, BR-14
(for assessments), BR-16 and BR-17. Everything marked Week 2 is designed here and specified, not built.
The Week 1 demo says so explicitly rather than hiding it.

## Permission matrix
Legend: Y = allowed, own = only your own records, creator = creator of the opportunity (ADMIN always allowed),
- = denied. "Open" means no login is required in Week 1 only.

| Endpoint | Anonymous | EMPLOYEE | MANAGER | ADMIN | Week |
|---|---|---|---|---|---|
| POST /api/auth/register, POST /api/auth/login | Y | Y | Y | Y | 2 |
| GET /api/skills | - | Y | Y | Y | 2 |
| GET /api/skills/{id} | - | Y | Y | Y | 2 |
| POST /api/skills | - | - | - | Y | 2 |
| POST /api/skill-assessments | Open in Week 1, - from Week 2 | own | own | own | 1 |
| GET /api/skill-assessments (list) | Open in Week 1, - from Week 2 | own | all | all | 1 |
| GET /api/skill-assessments/{id} | Open in Week 1, - from Week 2 | own | Y | Y | 1 |
| PUT /api/skill-assessments/{id} | Open in Week 1, - from Week 2 | own | own | own | 1 |
| DELETE /api/skill-assessments/{id} | Open in Week 1, - from Week 2 | own | own | own | 1 |
| PATCH /api/skill-assessments/{id}/verify, /reject | - | - | not own | not own | 2 |
| POST /api/opportunities | - | - | Y | Y | 2 |
| GET /api/opportunities, GET /api/opportunities/{id} | - | published only | Y | Y | 2 |
| PUT, DELETE /api/opportunities/{id} | - | - | creator | Y | 2 |
| POST, DELETE /api/opportunities/{id}/requirements | - | - | creator | Y | 2 |
| PATCH /api/opportunities/{id}/publish, /close | - | - | creator | Y | 2 |
| GET /api/opportunities/{id}/match | - | own | any employee | any employee | 2 |
| POST /api/applications | - | own | own | own | 2 |
| GET /api/applications | - | own | on your opportunities | all | 2 |
| PATCH /api/applications/{id}/shortlist, /reject | - | - | creator | Y | 2 |
| PATCH /api/applications/{id}/withdraw | - | own | own | own | 2 |
| PATCH /api/employees/{id}/role, /active | - | - | - | Y | 2 |

Notes on the matrix
- A manager can edit or withdraw only their own claims, never another employee's; managers verify, they do not rewrite.
- "any employee" on /match means a manager or admin may check the match for a chosen employee, while an employee can only check themselves.
- Role checks use the hierarchy (ADMIN includes MANAGER includes EMPLOYEE); ownership checks are written separately in the service layer.
- In Week 2 the list endpoint returns only the caller's own rows for an EMPLOYEE, so a guessed `employeeId` filter cannot expose someone else's assessments.

## Known limitations (stated, not hidden)
- There is no manager-to-report link, so any MANAGER can review any employee's assessments.
- A sole ADMIN cannot demote themselves, so there is always at least one.
- Week 1 has no authentication; endpoints are open and employeeId is in the request body.
- Withdrawing a PENDING claim deletes it permanently, so withdrawn claims leave no history.
- A VERIFIED level cannot be edited. Upgrading means submitting a new, higher-level claim (BR-17).
