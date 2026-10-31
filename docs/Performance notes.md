# Performance Notes (Week 3)

These notes describe what was done for performance, what was measured, and what was not.

## Pagination

Every list endpoint accepts two optional query parameters:

| Parameter | Default | Rule |
|-----------|---------|------|
| `page` | 0 | 0 or more (zero-based) |
| `size` | 20 | 1 to 100 |

- The body stays a plain JSON array, so existing clients keep working. The total number of matching rows is returned in the `X-Total-Count` response header.
- An invalid value (`size=0`, `size=101`, `page=-1`) returns 400 `VALIDATION_ERROR`; a non-numeric value returns 400 `BAD_REQUEST`.
- Applies to: `GET /api/skills`, `GET /api/opportunities`, `GET /api/applications`, `GET /api/skill-assessments` and `GET /api/employees/{id}/skill-assessments`.
- Sorting always ends with `id`, so rows with the same timestamp keep a stable order and no row is repeated or skipped between pages. This is covered by an automated test that walks every page and checks each row appears exactly once.
- Filters (`status`, `opportunityId`, and the visibility rules per role) run in the database, so a page is a page of the filtered result. Earlier the application list filtered in memory after loading everything.

### The opportunity list needs two queries
`Opportunity` loads its requirements with an entity graph. A page query that also joins a collection cannot use `LIMIT` in SQL: Hibernate would load all rows and page in memory. So the list first reads one page of opportunities (no join), then loads the requirements for just those ids and restores the page order. A test checks that a page still returns its requirements.

## Other query habits already in the code
- Skill names for a whole list are loaded with one query (not one per row).
- `spring.jpa.open-in-view` is `false`, so no database work happens during JSON rendering.
- List responses are built from DTOs, never from lazy entity graphs.

## Indexes (migration V6)

| Index | Purpose | Status |
|-------|---------|--------|
| `idx_opportunity_created (created_at DESC, id DESC)` | newest-first list | **Measured, used** |
| `idx_opportunity_status_created (status, created_at DESC, id DESC)` | status filter + newest first | Created; **not chosen by the planner** on the test data (see below) |
| `idx_application_created`, `idx_application_employee_created` | application lists | Created; **not measured** |
| `idx_assessment_created (created_at, id)` | assessment list (oldest first) | Created; **not measured** |
| `idx_opportunity_status` (V4) | - | **Dropped**: the composite index starts with `status`, so it is redundant by design |

Existing indexes from V1, V2 and V4 (status, skill, creator, requirement skill, application opportunity, employee/skill, plus the unique constraints) were kept.

## Measurement

Method: a throwaway PostgreSQL 16 container, migrations V1 to V5 applied, then 50,000 generated opportunities (a third each DRAFT, PUBLISHED, CLOSED, spread over 50,000 minutes). `EXPLAIN (ANALYZE, BUFFERS)` was run before and after applying V6 (and `ANALYZE` after). One run each, warm cache, local Docker, so absolute times are indicative only.

| Query | Before V6 | After V6 |
|-------|-----------|----------|
| `WHERE status = 'PUBLISHED' ORDER BY created_at DESC, id DESC LIMIT 20` | Bitmap Index Scan on `idx_opportunity_status`, 16,667 rows fetched, top-N heapsort. **9.136 ms**, 1,081 buffer hits | Index Scan on `idx_opportunity_created` with a filter, 58 rows read (20 kept, 38 skipped). **0.059 ms**, 4 buffers |
| `ORDER BY created_at DESC, id DESC LIMIT 20` | Seq Scan of 50,000 rows, top-N heapsort. **29.226 ms**, 1,060 buffer hits | Index Scan on `idx_opportunity_created`, 20 rows read. **0.028 ms**, 3 buffers |

What this shows: the full scan and the sort are gone, and the work no longer grows with the size of the table for the first page.

What it does not show:
- For the status query the planner chose the plain created-at index, not `idx_opportunity_status_created`. On this data a third of rows match, so 20 matches are found almost immediately. The composite index would be expected to matter only when a status is rare; that case was not measured. It stays for now and is a candidate for removal if write cost matters.
- The `applications` and `skill_assessments` indexes follow the same pattern but were not measured.
- Every index costs time on insert and update. With the demo data (a few rows) none of this is visible.

## Known limits
- **Offset paging gets slower on deep pages**: `page=1000` still makes the database walk past 20,000 rows. For this API's size that is acceptable; keyset (cursor) paging would be the next step if lists grow very large. The maximum page size of 100 bounds the cost of one request.
- `X-Total-Count` needs a count query per request, which is also proportional to the number of matching rows.
- No caching, no connection-pool tuning (Hikari defaults), and no load test were done.
- A MANAGER's application list uses an `OR` with a subquery, which was not measured.

## Reproducing the measurement
Start `postgres:16` in a scratch container, copy `src/main/resources/db/migration` into it, apply V1 to V5 with `psql`, insert the generated rows, run the two `EXPLAIN (ANALYZE, BUFFERS)` statements, apply V6, run `ANALYZE opportunities;` and run them again.