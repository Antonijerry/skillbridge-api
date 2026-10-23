
> **Historical document.** This is the Week 2 plan, written before any Week 2 code. The contract that describes what was built is [`api-spec.md`](api-spec.md). Where the build differs from this plan:
>
> | Planned | Built | Why |
> |---|---|---|
> | Match fields `requirementsTotal`, `details`, `eligible` | `totalRequirements` and `requirements`; no `eligible` | `eligible` is the same as `matchPercentage` being 100 |
> | Applying to your own opportunity gives 403 | 409 `INVALID_STATE` | BR-13 says violations of the apply rules return 409 |
> | `GET /api/opportunities/{id}/applications` | `GET /api/applications?opportunityId={id}` | The filter gives creators and admins the same result |
> | `POST /api/applications` returns a `Location` header | No `Location` header | There is no get-one-application endpoint |
> | Shortlist, reject and withdraw were a stretch goal | Built and verified | Done |

## Week 2: authentication, review workflow, opportunities and applications

Status: **specified before any Week 2 code.** Each endpoint below is built to match exactly; a change to a built endpoint edits this file in the same commit.

### Changes to the Week 1 contract
- Every endpoint except `POST /api/auth/register` and `POST /api/auth/login` needs `Authorization: Bearer <token>`.
- `POST /api/skill-assessments`: **`employeeId` is removed from the body.** The employee is the logged-in user. If a client still sends `employeeId`, it is ignored, never trusted.
- The assessment object gains `skillName` (read from the Skill relationship). Existing fields are unchanged.
- `GET /api/skills` and the other skill endpoints are built (they were Week 2 in the Week 1 spec).
- Pagination stays Week 3, as announced in the Week 1 spec; list bodies remain arrays.

### New error codes
| HTTP | code | When |
|---|---|---|
| 401 | UNAUTHORIZED | No token, malformed token, bad signature or expired token |
| 401 | INVALID_CREDENTIALS | Wrong email or password, or the account is inactive. One message for all three, so the API never reveals which emails exist |
| 403 | FORBIDDEN | Logged in, but this role or owner may not do this |
| 409 | EMAIL_TAKEN | Registration with an email that already exists (compared lower-case) |
| 409 | SKILL_NAME_TAKEN | A skill with that name exists (case-insensitive) |
| 409 | REQUIREMENT_EXISTS | The opportunity already requires that skill |
| 409 | INVALID_STATE | A lifecycle rule was broken: editing a non-DRAFT opportunity, publishing without requirements or a future closing date, closing something that is not PUBLISHED |
| 409 | ALREADY_APPLIED | The employee already applied to that opportunity |

The Week 1 codes (VALIDATION_ERROR, BAD_REQUEST, NOT_FOUND, METHOD_NOT_ALLOWED, PENDING_CLAIM_EXISTS, ALREADY_VERIFIED, ASSESSMENT_NOT_EDITABLE, SKILL_INACTIVE, INTERNAL_ERROR) are unchanged. Every error keeps the one format `{ "error": { "code", "message", "details?" } }`, including 401 and 403.

### Token
- JWT signed with HS256 using a secret from the environment (`JWT_SECRET`, at least 32 characters). The secret is never committed.
- Claims: `sub` = employee id, `role`, `iat`, `exp`. Lifetime 60 minutes. No refresh tokens (out of scope).
- The role in the token is only a hint for the request; the service re-reads the employee, so a deactivated account stops working.

### Authentication

#### `POST /api/auth/register`
Creates an account. The role is always `EMPLOYEE`; there is no way to register as MANAGER or ADMIN.

| Field | Type | Rules |
|---|---|---|
| fullName | string | required, 2 to 120 characters after trimming |
| email | string | required, valid email, at most 160 characters; stored lower-case |
| password | string | required, 8 to 72 characters (72 is BCrypt's limit) |

Success `201 Created`: `{ "id", "fullName", "email", "role": "EMPLOYEE", "createdAt" }`. The password and hash are never returned.

| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | Invalid field |
| 409 | EMAIL_TAKEN | Email already registered |

#### `POST /api/auth/login`
Body: `{ "email", "password" }`. Success `200 OK`:
```json
{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresInSeconds": 3600 }
```
| Status | code | Cause |
|---|---|---|
| 400 | VALIDATION_ERROR | Missing email or password |
| 401 | INVALID_CREDENTIALS | Unknown email, wrong password or inactive account |

### Employees

| Method and path | Purpose | Who | Success | Errors |
|---|---|---|---|---|
| `GET /api/employees/me` | The logged-in employee: id, fullName, email, role, active, createdAt | Any logged-in user | 200 | 401 |
| `GET /api/employees/{id}/skill-assessments` | An employee's assessments (same object and `status` filter as the main list) | The employee, or MANAGER or ADMIN | 200 | 400, 401, 403, 404 |
| `PATCH /api/employees/{id}/role` | Body `{ "role": "EMPLOYEE" \| "MANAGER" \| "ADMIN" }` | ADMIN | 200, returns the employee | 400, 401, 403, 404, 409 |
| `PATCH /api/employees/{id}/active` | Body `{ "active": true \| false }` | ADMIN | 200, returns the employee | 400, 401, 403, 404, 409 |

An ADMIN cannot demote or deactivate themselves (`409 INVALID_STATE`), so at least one active admin always exists. The employee object never contains `passwordHash`.

### Skills

| Method and path | Purpose | Who | Success | Errors |
|---|---|---|---|---|
| `GET /api/skills` | List skills. Optional `active=true` or `false`. Ordered by name | Any logged-in user | 200 | 400, 401 |
| `GET /api/skills/{id}` | One skill | Any logged-in user | 200 | 400, 401, 404 |
| `POST /api/skills` | Add a skill. Body: `name` (2 to 80 characters), `category` (1 to 60 characters), `description` (optional, at most 500) | ADMIN | 201 + `Location` | 400, 401, 403, 409 SKILL_NAME_TAKEN |

Skill object: `id`, `name`, `description`, `category`, `active`, `createdAt`.

### Skill assessments: Week 2 behaviour

Who may do what (the permission matrix in `docs/business-rules.md` is authoritative):

| Endpoint | EMPLOYEE | MANAGER | ADMIN |
|---|---|---|---|
| `POST /api/skill-assessments` | for self | for self | for self |
| `GET /api/skill-assessments` | own rows only (any `employeeId` filter is ignored) | all, filters work | all, filters work |
| `GET /api/skill-assessments/{id}` | own, else 403 | any | any |
| `PUT` and `DELETE /api/skill-assessments/{id}` | own, else 403 | own only | own only |
| `PATCH .../verify` and `.../reject` | 403 | not your own claim | not your own claim |

The Week 1 status codes and error codes of these endpoints still apply, with `401` and `403` added.

#### `PATCH /api/skill-assessments/{id}/verify`
Body (optional): `{ "reviewComment": "up to 500 characters" }`.
Effect: status becomes `VERIFIED`; `reviewerId` is the logged-in user; `reviewedAt` is set. Success `200 OK`, returns the assessment.

#### `PATCH /api/skill-assessments/{id}/reject`
Body (required): `{ "reviewComment": "1 to 500 characters" }`. A rejection must say why.
Effect: status becomes `REJECTED`; reviewer and time are recorded. Success `200 OK`, returns the assessment.

| Status | code | Cause (both endpoints) |
|---|---|---|
| 400 | VALIDATION_ERROR / BAD_REQUEST | Invalid body or id |
| 401 | UNAUTHORIZED | Not logged in |
| 403 | FORBIDDEN | Caller is an EMPLOYEE, or is reviewing their own claim |
| 404 | NOT_FOUND | No such assessment |
| 409 | ASSESSMENT_NOT_EDITABLE | The assessment is not PENDING (VERIFIED and REJECTED are final) |

### Opportunities

Lifecycle: `DRAFT` then `PUBLISHED` then `CLOSED`, never backwards.

Opportunity object: `id`, `title`, `description`, `createdBy`, `status`, `closingDate` (nullable `YYYY-MM-DD`), `createdAt`, `updatedAt`, and `requirements` (array of `{ "id", "skillId", "skillName", "minimumLevel" }`).

| Method and path | Purpose | Who | Success | Errors |
|---|---|---|---|---|
| `POST /api/opportunities` | Create a DRAFT. Body: `title` (3 to 150), `description` (10 to 2000), `closingDate` (optional, a date). `createdBy` is the caller | MANAGER, ADMIN | 201 + `Location` | 400, 401, 403 |
| `GET /api/opportunities` | List. Optional `status`. EMPLOYEE sees PUBLISHED only; MANAGER and ADMIN see all | Any logged-in user | 200 | 400, 401 |
| `GET /api/opportunities/{id}` | One, with requirements. An EMPLOYEE asking for a DRAFT or CLOSED one gets 403 | Any logged-in user | 200 | 400, 401, 403, 404 |
| `PUT /api/opportunities/{id}` | Replace title, description and closingDate. DRAFT only | Creator, ADMIN | 200 | 400, 401, 403, 404, 409 INVALID_STATE |
| `DELETE /api/opportunities/{id}` | Delete a DRAFT and its requirements | Creator, ADMIN | 204 | 400, 401, 403, 404, 409 INVALID_STATE |
| `POST /api/opportunities/{id}/requirements` | Add a required skill. Body: `skillId`, `minimumLevel`. DRAFT only | Creator, ADMIN | 201, returns the requirement | 400, 401, 403, 404, 409 REQUIREMENT_EXISTS / SKILL_INACTIVE / INVALID_STATE |
| `DELETE /api/opportunities/{id}/requirements/{requirementId}` | Remove a requirement. DRAFT only | Creator, ADMIN | 204 | 400, 401, 403, 404, 409 INVALID_STATE |
| `PATCH /api/opportunities/{id}/publish` | DRAFT to PUBLISHED. Needs at least one requirement and a `closingDate` in the future | Creator, ADMIN | 200 | 401, 403, 404, 409 INVALID_STATE |
| `PATCH /api/opportunities/{id}/close` | PUBLISHED to CLOSED | Creator, ADMIN | 200 | 401, 403, 404, 409 INVALID_STATE |

### Match: `GET /api/opportunities/{id}/match`
Optional query `employeeId`. An EMPLOYEE may only check themselves (omitting it means themselves; another id gives 403). A MANAGER or ADMIN may check any employee. The opportunity must be PUBLISHED for an EMPLOYEE; managers may check any status.

Rule (BR-11, BR-12): a requirement is met when the employee has at least one VERIFIED assessment for that skill at or above `minimumLevel`. A later PENDING or REJECTED claim never revokes an earlier VERIFIED one. `matchPercentage` = `requirementsMet * 100 / requirementsTotal`, rounded down. It informs the manager and never blocks applying.

Success `200 OK`:
```json
{
  "opportunityId": "uuid", "employeeId": "uuid",
  "requirementsTotal": 2, "requirementsMet": 1, "matchPercentage": 50, "eligible": false,
  "details": [
    { "skillId": "uuid", "skillName": "Java", "minimumLevel": "INTERMEDIATE", "verifiedLevel": "BEGINNER", "met": false },
    { "skillId": "uuid", "skillName": "SQL", "minimumLevel": "BEGINNER", "verifiedLevel": null, "met": false }
  ]
}
```
`verifiedLevel` is the highest VERIFIED level the employee holds for that skill, or `null`. `eligible` is true only when every requirement is met. Errors: 400, 401, 403, 404.

### Applications

Application object: `id`, `employeeId`, `opportunityId`, `status` (`SUBMITTED` for now), `createdAt`, `updatedAt`.

| Method and path | Purpose | Who | Success | Errors |
|---|---|---|---|---|
| `POST /api/applications` | Apply. Body: `{ "opportunityId" }`. The applicant is the caller | Any logged-in user | 201 + `Location` | 400, 401, 403, 404, 409 ALREADY_APPLIED / INVALID_STATE |
| `GET /api/applications` | EMPLOYEE: own applications. MANAGER: applications to opportunities they created. ADMIN: all | Any logged-in user | 200 | 401 |
| `GET /api/opportunities/{id}/applications` | Applicants to one opportunity | Creator, ADMIN | 200 | 400, 401, 403, 404 |

Rules: the opportunity must be PUBLISHED and its `closingDate` not passed (otherwise `409 INVALID_STATE`); one application per employee and opportunity (`409 ALREADY_APPLIED`); you cannot apply to an opportunity you created (`403 FORBIDDEN`).

**Stretch, built only if time remains:** `PATCH /api/applications/{id}/shortlist`, `/reject` (creator or ADMIN) and `/withdraw` (the applicant). The statuses SHORTLISTED, REJECTED and WITHDRAWN already exist in the schema.

### Seed data for Week 2
Migration V5 gives the three seeded employees a documented dummy password for **local demo use only**: Ada (EMPLOYEE), Tunde (MANAGER), Chidi (ADMIN). The password is written in the README, not here. The migration stores only its BCrypt hash.

### Known limitations (Week 2)
- No manager-to-report link: any MANAGER can review any employee's assessments.
- Withdrawing a PENDING claim deletes it permanently.
- Access tokens cannot be revoked before they expire (60 minutes); deactivating an account blocks the next login and the next request, because the employee is re-read.
- Login rate limiting arrives in Week 3.
