-- V6: support the paged list endpoints (filter + stable newest-first ordering + LIMIT/OFFSET).
-- Each index matches an ORDER BY used by a list query, so PostgreSQL can read rows already in order.

-- Opportunities: "all" and "by status", newest first.
CREATE INDEX idx_opportunity_created        ON opportunities (created_at DESC, id DESC);
CREATE INDEX idx_opportunity_status_created ON opportunities (status, created_at DESC, id DESC);
-- The new composite index starts with status, so the old single-column index is now redundant.
DROP INDEX idx_opportunity_status;

-- Applications: admin sees all, employees see their own, both newest first.
CREATE INDEX idx_application_created          ON applications (created_at DESC, id DESC);
CREATE INDEX idx_application_employee_created ON applications (employee_id, created_at DESC, id DESC);

-- Assessments: the list is oldest first.
CREATE INDEX idx_assessment_created ON skill_assessments (created_at, id);