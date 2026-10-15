# API specification

Status: **Week 1 endpoints are specified in full and will be implemented to match exactly.**
Week 2 endpoints are listed under "Planned" and are not built yet; their bodies are specified
at the start of Week 2, before any code. Any change to a built endpoint edits this file in the same commit.

## Conventions
- Base path `/api`. JSON in and out (`Content-Type: application/json`).
- Resource names are plural kebab-case; JSON fields are camelCase; enum values are UPPER_CASE.
- Ids are UUIDs. Timestamps are ISO-8601 UTC (`2026-10-16T09:30:00Z`); dates are `YYYY-MM-DD`.
- Text input is trimmed; a blank optional field counts as "not provided".
- Week 1 has **no authentication** and every endpoint is open. `employeeId` is sent in the body
  as a temporary stand-in; Week 2 removes it and takes the employee from the login token.
- Skill levels are ordered BEGINNER < INTERMEDIATE < ADVANCED < EXPERT. "Same or higher" below uses this order.

### Error format (every error, no exceptions)
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": [ { "field": "evidenceSummary", "message": "must be between 10 and 1000 characters" } ]
  }
}
```
`details` appears only for validation errors. Responses never contain stack traces, SQL or class names.

| HTTP | code | When |
|---|---|---|
| 400 | VALIDATION_ERROR | Body fails a validation rule (details lists each field) |
| 400 | BAD_REQUEST | Malformed JSON, unknown enum value, invalid UUID in path or query |
| 404 | NOT_FOUND | Valid UUID, but no such record (including the employee or skill in a body) |
| 405 | METHOD_NOT_ALLOWED | Method not supported on that path |
| 409 | PENDING_CLAIM_EXISTS | The employee already has a PENDING assessment for that skill |
| 409 | ALREADY_VERIFIED | The employee already has a VERIFIED assessment for that skill at the same or a higher level |
| 409 | ASSESSMENT_NOT_EDITABLE | Edit or delete attempted on a non-PENDING assessment |
| 409 | SKILL_INACTIVE | The skill exists but is inactive |
| 500 | INTERNAL_ERROR | Unexpected failure; details are logged server-side only |

### Skill assessment object
| Field | Type | Notes |
|---|---|---|
| id | uuid | |
| employeeId | uuid | Who made the claim |
| skillId | uuid | |
| claimedLevel | enum | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |
| evidenceUrl | string or null | http or https link, up to 500 characters |
| evidenceSummary | string | 10 to 1000 characters |
| status | enum | PENDING, VERIFIED, REJECTED |
| reviewerId | uuid or null | Null until reviewed (Week 2) |
| reviewedAt | timestamp or null | |
| reviewComment | string or null | |
| createdAt | timestamp | |
| updatedAt | timestamp | |

### Assessment history rules
These rules apply to every assessment endpoint and mirror business rules BR-2, BR-3, BR-17 in docs/business-rules.md.
1. An employee can have many assessments for the same skill over time, but **at most one PENDING** per employee and skill.
2. Only PENDING assessments can be edited (PUT) or withdrawn (DELETE). VERIFIED and REJECTED rows are immutable history.
3. A new claim is refused if the employee already has a VERIFIED assessment for that skill at the **same or a higher level**.
   Claiming a higher level than the verified one is allowed and starts a new PENDING claim; the old VERIFIED row keeps counting
   until the new claim is reviewed.
4. After a REJECTED claim, the employee resubmits with a new `POST`. The rejected row stays as history.
5. Withdrawing a PENDING claim deletes it permanently (known limitation, see the end of this file).

Check order on `POST` and on a level change in `PUT`: first `ALREADY_VERIFIED`, then `PENDING_CLAIM_EXISTS`.
The service checks both for a friendly message; the database partial unique index `uq_assessment_one_pending`
is the safety net for two simultaneous requests, and a violation of it is also returned as `409 PENDING_CLAIM_EXISTS`.

## Week 1 endpoints

### 1. Claim a skill: `POST /api/skill-assessments`
Purpose: an employee submits evidence for a skill. The new assessment is always `PENDING`.

Request body:
| Field | Type | Required | Rules |
|---|---|---|---|
| employeeId | uuid | yes | Must exist (temporary, removed in Week 2) |
| skillId | uuid | yes | Must exist and be active |
| claimedLevel | enum | yes | BEGINNER, INTERMEDIATE, ADVANCED or EXPERT |
| evidenceSummary | string | yes | 10 to 1000 characters after trimming |
| evidenceUrl | string | no | `http://` or `https://` followed by at least one character, at most 500 characters |

The summary and URL limits are identical to the database constraints `ck_assessment_summary_length` and
`ck_assessment_url_format`, so a request that passes validation can never fail on a database check.

Example request:
```json
{
  "employeeId": "11111111-1111-1111-1111-111111111111",
  "skillId": "aaaaaaaa-0000-0000-0000-000000000004",
  "claimedLevel": "ADVANCED",
  "evidenceSummary": "Built and deployed three Spring Boot services for the payroll team.",
  "evidenceUrl": "https://github.com/example/payroll-services"
}
```
Success: `201 Created`, header `Location: /api/skill-assessments/{id}`, body = skill assessment object
(`status` is `PENDING`, `reviewerId`, `reviewedAt` and `reviewComment` are `null`).

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | Missing or invalid field |
| 400 | BAD_REQUEST | Malformed JSON, unknown enum value |
| 404 | NOT_FOUND | employeeId or skillId does not exist |
| 409 | ALREADY_VERIFIED | The employee has a VERIFIED assessment for that skill at the same or a higher level |
| 409 | PENDING_CLAIM_EXISTS | The employee already has a PENDING assessment for that skill |
| 409 | SKILL_INACTIVE | The skill is inactive |

Example 409 body:
```json
{ "error": { "code": "PENDING_CLAIM_EXISTS", "message": "A pending claim for this skill already exists. Revise or withdraw it first." } }
```

### 2. List assessments: `GET /api/skill-assessments`
Purpose: list assessments, optionally filtered. An employee and skill can appear more than once (history).

Query parameters (all optional, combinable): `employeeId` (uuid), `skillId` (uuid), `status` (PENDING, VERIFIED or REJECTED).
Success: `200 OK`, body = array of skill assessment objects ordered by `createdAt` ascending; `[]` when nothing matches.
Pagination arrives in Week 3 as optional `page` and `size` parameters; the body stays an array.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | Invalid UUID or unknown status value |

### 3. Get one assessment: `GET /api/skill-assessments/{id}`
Success: `200 OK`, body = skill assessment object.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | `id` is not a valid UUID |
| 404 | NOT_FOUND | No assessment with that id |

### 4. Revise a claim: `PUT /api/skill-assessments/{id}`
Purpose: change the claimed level and evidence while the claim is still `PENDING`.
`PUT` replaces the editable fields: an omitted `evidenceUrl` clears it. Employee and skill cannot change.
Changing `claimedLevel` is checked like a new claim: it cannot be set to a level already covered by a VERIFIED
assessment for the same employee and skill. Otherwise an edit could bypass the `POST` rule.

Request body:
| Field | Type | Required | Rules |
|---|---|---|---|
| claimedLevel | enum | yes | As above |
| evidenceSummary | string | yes | 10 to 1000 characters after trimming |
| evidenceUrl | string | no | As above |

Success: `200 OK`, body = the updated skill assessment object (`updatedAt` changes).

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | Invalid field |
| 400 | BAD_REQUEST | Invalid UUID, malformed JSON, unknown enum value |
| 404 | NOT_FOUND | No assessment with that id |
| 409 | ASSESSMENT_NOT_EDITABLE | Status is not PENDING (VERIFIED and REJECTED are history) |
| 409 | ALREADY_VERIFIED | The new level is not above the employee's VERIFIED level for that skill |

### 5. Withdraw a claim: `DELETE /api/skill-assessments/{id}`
Purpose: remove a claim that has not been reviewed. Allowed only while `PENDING`.
The row is permanently deleted.
Success: `204 No Content`, empty body.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | `id` is not a valid UUID |
| 404 | NOT_FOUND | No assessment with that id |
| 409 | ASSESSMENT_NOT_EDITABLE | Status is not PENDING |


## Seed data (Week 1)
Dummy data only, loaded by a Flyway seed migration (planned for milestone M6). Fixed ids so examples can be copied and run.
- Employees: Ada (EMPLOYEE) `11111111-1111-1111-1111-111111111111`, Tunde (MANAGER) `22222222-2222-2222-2222-222222222222`, Chidi (ADMIN) `33333333-3333-3333-3333-333333333333`
- Skills: `aaaaaaaa-0000-0000-0000-00000000000N` for N = 1 to 6 (Java, SQL, REST API Design, Spring Boot, Docker, Technical Writing)
- Seed passwords are dummy BCrypt hashes, never real passwords.

## Planned (Week 2, not built; bodies specified before coding)
| Method | Path | Purpose | Who | Success | Main errors |
|---|---|---|---|---|---|
| POST | /api/auth/register | Create an EMPLOYEE account | Anyone | 201 | 400, 409 |
| POST | /api/auth/login | Obtain a JWT | Anyone | 200 | 400, 401 |
| GET | /api/skills/{id} | One skill | Logged in | 200 | 400, 404 |
| POST | /api/skills | Add a skill | ADMIN | 201 | 400, 401, 403, 409 |
| PATCH | /api/skill-assessments/{id}/verify | Verify a claim (not your own) | MANAGER, ADMIN | 200 | 401, 403, 404, 409 |
| PATCH | /api/skill-assessments/{id}/reject | Reject a claim with a comment | MANAGER, ADMIN | 200 | 400, 401, 403, 404, 409 |
| POST | /api/opportunities | Create a DRAFT opportunity | MANAGER, ADMIN | 201 | 400, 401, 403 |
| GET | /api/opportunities, /{id} | Browse opportunities | Logged in | 200 | 401, 404 |
| PUT, DELETE | /api/opportunities/{id} | Edit or delete a DRAFT | Creator, ADMIN | 200, 204 | 400, 401, 403, 404, 409 |
| POST, DELETE | /api/opportunities/{id}/requirements | Add or remove a required skill | Creator, ADMIN | 201, 204 | 400, 401, 403, 404, 409 |
| PATCH | /api/opportunities/{id}/publish, /close | Lifecycle changes | Creator, ADMIN | 200 | 401, 403, 404, 409 |
| GET | /api/opportunities/{id}/match | Match percentage from VERIFIED skills | Logged in | 200 | 401, 403, 404 |
| POST | /api/applications | Apply to a published opportunity | Logged in | 201 | 400, 401, 404, 409 |
| GET | /api/applications | Own applications, or applicants to your opportunities | Logged in | 200 | 401 |
| PATCH | /api/applications/{id}/shortlist, /reject, /withdraw | Change application status | Creator or owner | 200 | 401, 403, 404, 409 |
| PATCH | /api/employees/{id}/role, /active | Manage accounts | ADMIN | 200 | 400, 401, 403, 404 |

## Known limitations (stated, not hidden)
- Week 1 has no authentication. Anyone can send any `employeeId`; Week 2 removes the field and uses the login token.
- Withdrawing (DELETE) a PENDING claim deletes it permanently, so withdrawn claims leave no history.
- A VERIFIED level cannot be edited. To claim a higher level, submit a new claim.
