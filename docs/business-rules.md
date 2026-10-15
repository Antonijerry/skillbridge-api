# Business rules and permissions

Endpoint paths here are provisional until docs/api-spec.md is written.

## Roles
- EMPLOYEE: the default. Registration can only ever create this role.
- MANAGER: everything an employee can do, plus reviewing assessments and managing opportunities.
- ADMIN: everything a manager can do, plus managing skills and employee accounts.
  Each role includes the one below it.

## Rules
| ID | Rule | Enforced in |
|---|---|---|
| BR-1 | An employee submits assessments only for themselves; the employee comes from the login token, never the request body (Week 1 uses the body as a temporary stand-in) | Service |
| BR-2 | One assessment per employee and skill | Database unique constraint, plus a friendly check in the service (409) |
| BR-3 | A new assessment starts as PENDING. Edit and withdraw are allowed only while PENDING (409 otherwise) | Service |
| BR-4 | Only a MANAGER or ADMIN can verify or reject; the reviewer must not be the assessment's own employee | Security plus a database CHECK |
| BR-5 | A reviewed assessment records reviewer and reviewedAt; VERIFIED and REJECTED are final in v1 (a rejected claim may be revised and resubmitted in Week 2) | Service plus database CHECK |
| BR-6 | Inactive skills cannot be newly claimed or required | Service |
| BR-7 | Inactive employees cannot log in | Authentication |
| BR-8 | Opportunity lifecycle: DRAFT -> PUBLISHED -> CLOSED, no going back | Service |
| BR-9 | Publishing needs at least one requirement and a closing date in the future | Service |
| BR-10 | Requirements can be added or removed only while DRAFT; one requirement per skill per opportunity | Service plus database unique constraint |
| BR-11 | Eligibility counts only VERIFIED assessments at or above the minimum level | Service query |
| BR-12 | Match percentage = requirements met / total requirements x 100, rounded down. It informs the manager and does not block applying | Service |
| BR-13 | Applying needs a PUBLISHED opportunity whose closing date has not passed; one application per employee and opportunity; you cannot apply to your own opportunity | Service plus database unique constraint |
| BR-14 | Parents with children are never silently deleted: skills and employees are deactivated instead; only DRAFT opportunities and PENDING assessments can be deleted | Foreign keys with RESTRICT, plus service |
| BR-15 | Ownership failures return 403 (not logged in: 401). IDs are UUIDs, but ownership checks are the real protection | Security plus service |

## Permission matrix
Legend: Y = allowed, own = only your own records, creator = creator of the opportunity (or ADMIN), - = denied.

| Endpoint | Anonymous | EMPLOYEE | MANAGER | ADMIN | Week |
|---|---|---|---|---|---|
| POST /api/auth/register, POST /api/auth/login | Y | Y | Y | Y | 2 |
| GET /api/skills, GET /api/skills/{id} | - (Week 1: open) | Y | Y | Y | 1 |
| POST/PUT /api/skills, PATCH /api/skills/{id}/active | - | - | - | Y | 2 |
| POST /api/skill-assessments | - | own | own | own | 1 |
| GET /api/skill-assessments (list) | - | own | all | all | 1 |
| GET /api/skill-assessments/{id} | - | own | Y | Y | 1 |
| PUT, DELETE /api/skill-assessments/{id} | - | own | own | own | 1 |
| PATCH /api/skill-assessments/{id}/verify or /reject | - | - | not own | not own | 2 |
| POST /api/opportunities | - | - | Y | Y | 2 |
| GET /api/opportunities, GET /{id} | - | published only | Y | Y | 2 |
| PUT/DELETE /api/opportunities/{id}, requirements add/remove | - | - | creator | Y | 2 |
| PATCH /api/opportunities/{id}/publish or /close | - | - | creator | Y | 2 |
| GET /api/opportunities/{id}/match | - | own | any employee | any employee | 2 |
| POST /api/applications | - | own | own | own | 2 |
| GET /api/applications | - | own | creator's opportunities | all | 2 |
| PATCH /api/applications/{id}/shortlist or /reject | - | - | creator | Y | 2 |
| PATCH /api/applications/{id}/withdraw | - | own | own | own | 2 |
| PATCH /api/employees/{id}/role, /active | - | - | - | Y | 2 |

## Known limitations (stated, not hidden)
- There is no manager-to-report link, so any MANAGER can review any employee's assessments.
- A sole ADMIN cannot demote themselves, so there is always at least one.
- Week 1 has no authentication; the endpoints are open and employeeId is in the request body.