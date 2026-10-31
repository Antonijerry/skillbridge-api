# API specification

Status: **Week 1, Week 2 and Week 3 behaviour is implemented and verified against this file.**
Week 3 added pagination on list endpoints, rate limiting on the auth routes and deliberate CORS (see "Pagination, rate limiting and CORS" below). This file and the Postman collection in `postman/` are the endpoint documentation. Any change to a built endpoint edits this file in the same commit.

## Conventions
- Base path `/api`. JSON in and out (`Content-Type: application/json`).
- Resource names are plural kebab-case; JSON fields are camelCase; enum values are UPPER_CASE.
- Ids are UUIDs. Timestamps are ISO-8601 UTC (`2026-10-16T09:30:00Z`); dates are `YYYY-MM-DD`.
- Text input is trimmed; a blank optional field counts as "not provided".
- Skill levels are ordered BEGINNER < INTERMEDIATE < ADVANCED < EXPERT. "Same or higher" below uses this order.
- Lists are ordered as stated per endpoint and are paginated (see "Pagination, rate limiting and CORS" below).

### Authentication and roles (Week 2)
- Every endpoint except `POST /api/auth/register` and `POST /api/auth/login` needs `Authorization: Bearer <token>`.
- The token is a signed JWT (HS256) valid for 60 minutes. It identifies the caller; **a client never sends its own employee id**.
- Roles: EMPLOYEE, MANAGER, ADMIN. A MANAGER or ADMIN is called "staff". Registration always creates an EMPLOYEE.
- The caller's role and active status are read from the database on every request, not from the token. A role change applies at once, and a deactivated account's token stops working at once (401).
- Check order on endpoints with roles or ownership: **wrong role is 403, then a missing record is 404, then wrong ownership is 403.** Where an endpoint differs, it says so.

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
`details` appears only for validation errors. Responses never contain stack traces, SQL or class names. Errors are always JSON, whatever the client's `Accept` header says.

| HTTP | code | When |
|---|---|---|
| 400 | VALIDATION_ERROR | Body fails a validation rule (details lists each field) |
| 400 | BAD_REQUEST | Missing or malformed JSON, unknown enum value, invalid UUID or date, invalid query value |
| 401 | UNAUTHORIZED | No token, an invalid or expired token, or the account is deactivated. Response header `WWW-Authenticate: Bearer` |
| 401 | INVALID_CREDENTIALS | Login failed (unknown email, wrong password or inactive account: always the same answer) |
| 403 | FORBIDDEN | Wrong role, or not the owner |
| 404 | NOT_FOUND | Valid UUID, but no such record (including the skill in a body), or an unknown path |
| 405 | METHOD_NOT_ALLOWED | Method not supported on that path |
| 406 | NOT_ACCEPTABLE | The client asked for something other than JSON |
| 409 | EMAIL_TAKEN | Registration with an email that already has an account |
| 409 | SKILL_NAME_TAKEN | A skill with the same name (case-insensitive) exists |
| 409 | PENDING_CLAIM_EXISTS | The employee already has a PENDING assessment for that skill |
| 409 | ALREADY_VERIFIED | The employee already has a VERIFIED assessment for that skill at the same or a higher level |
| 409 | ASSESSMENT_NOT_EDITABLE | Edit, delete or review attempted on a non-PENDING assessment |
| 409 | SKILL_INACTIVE | The skill exists but is inactive |
| 409 | REQUIREMENT_EXISTS | The opportunity already requires that skill |
| 409 | INVALID_STATE | The action is not allowed in the current state (opportunity or application status, closing date, own opportunity, an admin acting on themselves) |
| 409 | ALREADY_APPLIED | One application per employee and opportunity, including a withdrawn one |
| 415 | UNSUPPORTED_MEDIA_TYPE | The request body is not `application/json` |
| 429 | TOO_MANY_REQUESTS | Too many `POST /api/auth/*` requests from one address in the current window. Response header `Retry-After` gives the seconds to wait |
| 500 | INTERNAL_ERROR | Unexpected failure; details are logged server-side only |

### Interactive documentation (Week 3)
- Swagger UI: `http://localhost:8080/swagger-ui.html`. Generated from the controllers by springdoc-openapi, so it always matches the running code. Use **Authorize** to paste a bearer token.
- Raw OpenAPI document of the running app: `/v3/api-docs`.
- Hand-written contract with the full error codes and examples: `docs/openapi.yaml` (OpenAPI 3.0.3).
- Both are public and can be switched off with `SWAGGER_ENABLED=false`. The generated document does not list every error code (429 and the 409 variants); this file and `openapi.yaml` do.

### Pagination, rate limiting and CORS (Week 3)

**Pagination.** Every list endpoint (`GET /api/skills`, `/api/opportunities`, `/api/applications`, `/api/skill-assessments` and `/api/employees/{id}/skill-assessments`) accepts:

| Query parameter | Default | Rule |
|---|---|---|
| `page` | 0 | zero-based, 0 or more |
| `size` | 20 | 1 to 100 |

- The body stays a plain JSON array. The total number of rows matching the filters is in the response header `X-Total-Count`.
- A page past the end returns `[]` with the total still reported (status 200).
- `size=0`, `size=101` or `page=-1` returns 400 `VALIDATION_ERROR`; a non-numeric value returns 400 `BAD_REQUEST`.
- Ordering is stable: the order stated per endpoint, then by id, so no row is repeated or skipped between pages. Filters and role visibility apply before paging.

Example:
```http
GET /api/skills?page=1&size=2
Authorization: Bearer <token>

200 OK
X-Total-Count: 6
[ { "id": "...", "name": "REST API Design", ... }, { "id": "...", "name": "Spring Boot", ... } ]
```

**Rate limiting.** `POST /api/auth/register` and `POST /api/auth/login` allow 20 requests per 60 seconds per client address (configurable: `RATE_LIMIT_AUTH_MAX`, `RATE_LIMIT_AUTH_WINDOW_SECONDS`). The next request returns 429 `TOO_MANY_REQUESTS` with `Retry-After`. The limit is in memory (per instance, reset on restart) and keyed on the connection address, not on `X-Forwarded-For`.

**CORS.** A browser on another origin may call the API only if its origin is listed in `CORS_ALLOWED_ORIGINS` (comma separated). The default is none. Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS; allowed request headers: Authorization, Content-Type, Accept; exposed response headers: `X-Total-Count`, `Retry-After`; credentials are not allowed (tokens are sent in a header, not a cookie). Requests that are not from a browser are unaffected.

### Objects

**Skill assessment**
| Field | Type | Notes |
|---|---|---|
| id | uuid | |
| employeeId | uuid | Who made the claim |
| skillId | uuid | |
| skillName | string | The skill's name |
| claimedLevel | enum | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |
| evidenceUrl | string or null | http or https link, up to 500 characters |
| evidenceSummary | string | 10 to 1000 characters |
| status | enum | PENDING, VERIFIED, REJECTED |
| reviewerId | uuid or null | Null until reviewed |
| reviewedAt | timestamp or null | |
| reviewComment | string or null | |
| createdAt | timestamp | |
| updatedAt | timestamp | |

**Employee**: `id`, `fullName`, `email`, `role`, `active`, `createdAt`. The password hash is never returned.

**Skill**: `id`, `name`, `description` (or null), `category`, `active`, `createdAt`.

**Opportunity**: `id`, `title`, `description`, `createdBy` (employee id), `status` (DRAFT, PUBLISHED, CLOSED), `closingDate` (or null), `createdAt`, `updatedAt`, `requirements` (list of requirement objects).

**Requirement**: `id`, `skillId`, `skillName`, `minimumLevel`.

**Application**: `id`, `employeeId`, `opportunityId`, `status` (SUBMITTED, SHORTLISTED, REJECTED, WITHDRAWN), `createdAt`, `updatedAt`.

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

---

## Week 1 endpoints (skill assessments)
In Week 2 these sit behind authentication. The employee is always the logged-in user. Every endpoint below can also return `401 UNAUTHORIZED`.

### 1. Claim a skill: `POST /api/skill-assessments`
Purpose: the logged-in employee submits evidence for a skill. The new assessment is always `PENDING`.
Who: any logged-in user. The claim belongs to the caller; there is no `employeeId` in the body.

Request body:
| Field | Type | Required | Rules |
|---|---|---|---|
| skillId | uuid | yes | Must exist and be active |
| claimedLevel | enum | yes | BEGINNER, INTERMEDIATE, ADVANCED or EXPERT |
| evidenceSummary | string | yes | 10 to 1000 characters after trimming |
| evidenceUrl | string | no | `http://` or `https://` followed by at least one character, at most 500 characters |

The summary and URL limits are identical to the database constraints `ck_assessment_summary_length` and
`ck_assessment_url_format`, so a request that passes validation can never fail on a database check.

Example request:
```json
{
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
| 401 | UNAUTHORIZED | No or invalid token |
| 404 | NOT_FOUND | skillId does not exist |
| 409 | SKILL_INACTIVE | The skill is inactive |
| 409 | ALREADY_VERIFIED | The caller has a VERIFIED assessment for that skill at the same or a higher level |
| 409 | PENDING_CLAIM_EXISTS | The caller already has a PENDING assessment for that skill |

Example 409 body:
```json
{ "error": { "code": "PENDING_CLAIM_EXISTS", "message": "A pending claim for this skill already exists. Revise or withdraw it first." } }
```

### 2. List assessments: `GET /api/skill-assessments`
Purpose: list assessments, optionally filtered. An employee and skill can appear more than once (history).
Who: any logged-in user. An **EMPLOYEE sees only their own assessments** (the `employeeId` parameter is ignored). A MANAGER or ADMIN sees everyone's, and may filter by `employeeId`.

Query parameters (all optional, combinable): `employeeId` (uuid, staff only), `skillId` (uuid), `status` (PENDING, VERIFIED or REJECTED).
Success: `200 OK`, body = array of skill assessment objects ordered by `createdAt` ascending; `[]` when nothing matches.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | Invalid UUID or unknown status value |
| 401 | UNAUTHORIZED | No or invalid token |

### 3. Get one assessment: `GET /api/skill-assessments/{id}`
Who: the owner, or any MANAGER or ADMIN. Success: `200 OK`, body = skill assessment object.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | `id` is not a valid UUID |
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | An EMPLOYEE who does not own the assessment |
| 404 | NOT_FOUND | No assessment with that id |

### 4. Revise a claim: `PUT /api/skill-assessments/{id}`
Purpose: change the claimed level and evidence while the claim is still `PENDING`.
Who: **the owner only** (a manager or admin cannot edit someone else's claim).
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
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | Not the owner |
| 404 | NOT_FOUND | No assessment with that id |
| 409 | ASSESSMENT_NOT_EDITABLE | Status is not PENDING (VERIFIED and REJECTED are history) |
| 409 | ALREADY_VERIFIED | The new level is not above the employee's VERIFIED level for that skill |

### 5. Withdraw a claim: `DELETE /api/skill-assessments/{id}`
Purpose: remove a claim that has not been reviewed. Allowed only while `PENDING`, and only for the owner.
The row is permanently deleted.
Success: `204 No Content`, empty body.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | `id` is not a valid UUID |
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | Not the owner |
| 404 | NOT_FOUND | No assessment with that id |
| 409 | ASSESSMENT_NOT_EDITABLE | Status is not PENDING |

---

## Week 2 endpoints

### Authentication

**6. Register: `POST /api/auth/register`** (no token)
Creates an account. The role is always EMPLOYEE; a client cannot choose it.
Request:
| Field | Type | Required | Rules |
|---|---|---|---|
| fullName | string | yes | 2 to 120 characters |
| email | string | yes | Valid email, at most 160 characters; unique, compared case-insensitively |
| password | string | yes | 8 to 72 characters, and at most 72 **bytes** (a BCrypt limit) |

Success: `201 Created`, body = `{"id", "fullName", "email", "role", "createdAt"}`. The password is stored only as a BCrypt hash and is never returned.

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | Invalid field, or a password longer than 72 bytes ("Password must be at most 72 bytes") |
| 409 | EMAIL_TAKEN | An account with this email exists |

**7. Login: `POST /api/auth/login`** (no token)
Request: `{"email": "…", "password": "…"}` (both required).
Success: `200 OK`, body = `{"accessToken": "…", "tokenType": "Bearer", "expiresInSeconds": 3600}`.

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | A field is missing or blank |
| 401 | INVALID_CREDENTIALS | Unknown email, wrong password or inactive account (the same answer for all three) |

**8. Who am I: `GET /api/employees/me`**
Any logged-in user. Success: `200 OK`, body = employee object. Error: 401.

### Employees and accounts

**9. A person's assessments: `GET /api/employees/{id}/skill-assessments`**
Who: the employee themselves, or any MANAGER or ADMIN. Optional `status` filter. Ordered by `createdAt` ascending.
Success: `200 OK`, array of skill assessment objects.

| Status | code | Cause |
|---|---|---|
| 400 | BAD_REQUEST | Invalid UUID or unknown status |
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | An EMPLOYEE asking for someone else's assessments |
| 404 | NOT_FOUND | No such employee (for staff callers) |

**10. Change a role: `PATCH /api/employees/{id}/role`**
ADMIN only. Request: `{"role": "EMPLOYEE" | "MANAGER" | "ADMIN"}` (required). Success: `200 OK`, body = employee object.

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR / BAD_REQUEST | Missing or unknown role, malformed id |
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | Not an admin |
| 404 | NOT_FOUND | No such employee |
| 409 | INVALID_STATE | An admin cannot demote themselves |

**11. Activate or deactivate: `PATCH /api/employees/{id}/active`**
ADMIN only. Request: `{"active": true | false}` (required). Success: `200 OK`, body = employee object.
A deactivated account cannot log in, and its existing tokens stop working at once. Accounts are deactivated, never deleted.
Errors: as above; `409 INVALID_STATE` when an admin deactivates themselves.

### Skills

**12. List: `GET /api/skills`** Any logged-in user. Optional `active=true|false`. Ordered by name. Success: `200 OK`, array of skill objects.
**13. One: `GET /api/skills/{id}`** Any logged-in user. Errors: 400 (invalid id), 401, 404.
**14. Create: `POST /api/skills`** ADMIN only.
Request: `{"name": "Kubernetes", "category": "DevOps", "description": "Orchestrating containers"}`.
| Field | Required | Rules |
|---|---|---|
| name | yes | 2 to 80 characters; unique ignoring case |
| category | yes | At most 60 characters |
| description | no | At most 500 characters |

Success: `201 Created`, header `Location: /api/skills/{id}`, body = skill object.
Errors: 400 `VALIDATION_ERROR`, 401, 403, 409 `SKILL_NAME_TAKEN`. (The role is checked before the lookup; a request that fails validation still gets 400 before the role check, a known limitation.)

### Reviewing claims

**15. Verify: `PATCH /api/skill-assessments/{id}/verify`**
Who: MANAGER or ADMIN, **not the claim's owner**. Request body optional: `{"reviewComment": "…"}` (at most 500 characters).
Success: `200 OK`, body = assessment (`status` VERIFIED, `reviewerId`, `reviewedAt` and `reviewComment` set).

**16. Reject: `PATCH /api/skill-assessments/{id}/reject`**
Same who. Request: `{"reviewComment": "…"}`, **required**, at most 500 characters ("a rejection must say why"). Success: `200 OK`, status REJECTED.

| Status | code | Cause (both endpoints) |
|---|---|---|
| 400 | VALIDATION_ERROR / BAD_REQUEST | Reject without a comment, comment too long, malformed id |
| 401 | UNAUTHORIZED | No or invalid token |
| 403 | FORBIDDEN | Not staff (checked first, before the lookup), or reviewing your own claim |
| 404 | NOT_FOUND | No assessment with that id |
| 409 | ASSESSMENT_NOT_EDITABLE | The assessment is not PENDING |

### Opportunities

**17. Create: `POST /api/opportunities`** MANAGER or ADMIN. Always created as DRAFT.
Request:
| Field | Required | Rules |
|---|---|---|
| title | yes | 3 to 150 characters |
| description | yes | 10 to 2000 characters |
| closingDate | no | `YYYY-MM-DD`. Not checked here; it must be in the future to publish |

Success: `201 Created`, header `Location: /api/opportunities/{id}`, body = opportunity object. Errors: 400, 401, 403.

**18. List: `GET /api/opportunities`** Any logged-in user. Optional `status` (DRAFT, PUBLISHED, CLOSED). Newest first.
An **EMPLOYEE sees only PUBLISHED** opportunities (asking for DRAFT or CLOSED returns `[]`). A MANAGER or ADMIN sees every status and every creator's opportunities.

**19. One: `GET /api/opportunities/{id}`** Staff: any opportunity. EMPLOYEE: only PUBLISHED, otherwise `403`. Errors: 400, 401, 403, 404.

**20. Edit: `PUT /api/opportunities/{id}`** and **21. Delete: `DELETE /api/opportunities/{id}`**
Who: the creator or an ADMIN (a MANAGER who is not the creator gets 403). Only a DRAFT: otherwise `409 INVALID_STATE`. `PUT` takes the same body as create. Deleting a DRAFT also deletes its requirements. Success: `200` with the opportunity, or `204`.

**22. Add a required skill: `POST /api/opportunities/{id}/requirements`**
Who: the creator or an ADMIN. Request: `{"skillId": "…", "minimumLevel": "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT"}` (both required).
Success: `201 Created`, body = requirement object.

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR / BAD_REQUEST | Missing field, unknown level, malformed id |
| 401, 403 | | No token; not staff, or not the creator |
| 404 | NOT_FOUND | No such opportunity or skill |
| 409 | INVALID_STATE | The opportunity is not DRAFT |
| 409 | SKILL_INACTIVE | The skill is inactive |
| 409 | REQUIREMENT_EXISTS | The skill is already required |

**23. Remove a requirement: `DELETE /api/opportunities/{id}/requirements/{requirementId}`**
Who: the creator or an ADMIN. Success: `204`. Errors: 401, 403, 404 (no such opportunity or requirement), 409 `INVALID_STATE` (not DRAFT).

**24. Publish: `PATCH /api/opportunities/{id}/publish`** and **25. Close: `PATCH /api/opportunities/{id}/close`**
Who: the creator or an ADMIN. Lifecycle is DRAFT to PUBLISHED to CLOSED, with no going back.
Publish needs at least one requirement and a `closingDate` strictly after today (UTC). Close works only on a PUBLISHED opportunity. Success: `200 OK`, body = opportunity object.
Errors: 401, 403, 404, 409 `INVALID_STATE` (wrong status, no requirements, missing or past closing date).

### Match

**26. Match: `GET /api/opportunities/{id}/match`**
Who: any logged-in user, for themselves. A MANAGER or ADMIN may add `?employeeId={uuid}` to check someone else; anyone else who passes another id gets 403. An EMPLOYEE gets 403 for an opportunity that is not PUBLISHED.
Only VERIFIED assessments count. A requirement is met when the employee has a VERIFIED assessment for that skill at or above the minimum level; PENDING and REJECTED claims never count. The percentage is requirements met divided by total requirements times 100, **rounded down**. It never blocks applying. It is computed on each request and never stored.
Success: `200 OK`:
```json
{
   "opportunityId": "…", "employeeId": "…",
   "totalRequirements": 3, "requirementsMet": 2, "matchPercentage": 66,
   "requirements": [
      {"skillId": "…", "skillName": "Java", "minimumLevel": "INTERMEDIATE", "verifiedLevel": "ADVANCED", "met": true},
      {"skillId": "…", "skillName": "SQL", "minimumLevel": "BEGINNER", "verifiedLevel": "BEGINNER", "met": true},
      {"skillId": "…", "skillName": "Docker", "minimumLevel": "ADVANCED", "verifiedLevel": "INTERMEDIATE", "met": false}
   ]
}
```
`verifiedLevel` is the employee's highest VERIFIED level for the skill, or `null`.
Errors: 400 (malformed id), 401, 403, 404 (no such opportunity or employee).

### Applications

**27. Apply: `POST /api/applications`** Any logged-in user. Request: `{"opportunityId": "{uuid}"}` (required).
Success: `201 Created`, body = application object (`status` SUBMITTED).

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR / BAD_REQUEST | Missing or malformed id |
| 401 | UNAUTHORIZED | No or invalid token |
| 404 | NOT_FOUND | No such opportunity |
| 409 | INVALID_STATE | The opportunity is not PUBLISHED, its closing date has passed, or it is the applicant's own opportunity |
| 409 | ALREADY_APPLIED | The employee already applied to this opportunity (a withdrawn application counts) |

**28. List: `GET /api/applications`** Optional filters `status` (SUBMITTED, SHORTLISTED, REJECTED, WITHDRAWN) and `opportunityId`. Newest first.
An EMPLOYEE sees their own applications. A MANAGER sees their own plus applications to opportunities they created. An ADMIN sees all.
Errors: 400 (bad filter), 401.

**29. Shortlist, reject, withdraw: `PATCH /api/applications/{id}/shortlist`, `/reject`, `/withdraw`** No body. Success: `200 OK`, body = application object.

| Action | Who | Allowed from | Result |
|---|---|---|---|
| shortlist | MANAGER who created the opportunity, or ADMIN | SUBMITTED | SHORTLISTED |
| reject | MANAGER who created the opportunity, or ADMIN | SUBMITTED, SHORTLISTED | REJECTED |
| withdraw | The applicant only | SUBMITTED, SHORTLISTED | WITHDRAWN |

Errors: 400 (malformed id), 401, 403, 404, 409 `INVALID_STATE` (transition not allowed). Shortlist and reject check the role first (403), then the application (404), then the opportunity's creator (403). Withdraw checks existence (404), then ownership (403).

---

## Seed data
Dummy data only, loaded by Flyway migrations (V3 seed, V5 demo passwords). Fixed ids so examples can be copied and run.
- Employees: Ada (EMPLOYEE) `11111111-1111-1111-1111-111111111111`, Tunde (MANAGER) `22222222-2222-2222-2222-222222222222`, Chidi (ADMIN) `33333333-3333-3333-3333-333333333333`. Emails `ada@`, `tunde@` and `chidi@skillbridge.local`.
- Skills: `aaaaaaaa-0000-0000-0000-00000000000N` for N = 1 to 6 (Java, SQL, REST API Design, Spring Boot, Docker, Technical Writing).
- Ada has two seeded assessments: Java BEGINNER (VERIFIED by Tunde) and SQL INTERMEDIATE (PENDING).
- The three accounts share the documented dummy demo password `Demo-Pass-123`, stored as a BCrypt hash. This is demo data only.

## Known limitations (stated, not hidden)
- Withdrawing (DELETE) a PENDING claim deletes it permanently, so withdrawn claims leave no history.
- A VERIFIED level cannot be edited. To claim a higher level, submit a new claim.
- Pagination is offset based: very deep pages (for example `page=10000`) get slower as the table grows. The page size is capped at 100. A cursor (keyset) scheme would be the next step.
- The rate limit is held in memory per instance, so it resets on restart and is not shared between several instances. Behind a reverse proxy all clients share the proxy's address until forwarded headers are configured safely.
- A withdrawn application cannot be re-submitted: one application per employee and opportunity.
- Two reviewers acting on the same claim at the same instant: the last write wins.
- A URL with invalid percent-encoding is rejected by the server with a plain HTML 400 page, outside the error format.
- The seeded demo accounts share a known password and must not ship to a real deployment.