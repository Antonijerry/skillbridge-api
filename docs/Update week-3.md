# Week 3 update: SkillBridge API

**Author:** ANTHONY JERRY K. · **Tag:** `week-3` · **Repository:** https://github.com/Antonijerry/skillbridge-api

## Delivered
- **Automated tests:** 97 tests (49 unit, 48 integration) that run with `.\mvnw.cmd test`. The integration tests use a throwaway PostgreSQL 16 container and cover 400, 401, 403, 404, 405, 409 and 429 cases and a full journey (claim, verify, publish, match, apply, shortlist, withdraw). Instruction coverage rose from 40.1% to 76.1% (77% in the final run of 97 tests) when I closed the biggest gap (the assessment workflow, 3% to 77.7%). The coverage note says what is not tested and why.
- **Security basics:** rate limiting on register and login (429 with `Retry-After`), CORS closed by default and configurable, tests for injection-looking input, markup in text and error bodies that leak nothing, and a security note that lists the known gaps.
- **Performance basics:** pagination on every list endpoint (`page`, `size`, `X-Total-Count`, stable order) and migration V6 with list indexes, measured with `EXPLAIN ANALYZE` on 50,000 rows.
- **Documentation and delivery:** updated API spec, an OpenAPI file, Swagger UI generated from the code (springdoc, switchable with `SWAGGER_ENABLED`), an updated Postman collection with paging checks, a setup guide, a case-study README, a walkthrough video, a technical reflection and a declaration.

## Changed after feedback
Week 1 scores arrived after the Week 2 submission (scores only, no comments). The lowest were the mentor's 3/5 on the security approach, decision rationale, testing and iteration, and documentation, and the peer's 2/5 on evidence gathering. Each is answered in `docs/feedback-response.md` with what I changed.

## Key technical decisions
- Tests run against a real PostgreSQL (Testcontainers) and not H2, because the schema uses PostgreSQL features (a partial unique index, CHECK constraints, `gen_random_uuid`, `timestamptz`). Cost: Docker is needed to run the tests.
- The opportunity list is two queries (page the opportunities, then load their requirements) because a paged query that also fetches a collection is paged in memory by Hibernate.
- Pagination keeps the array body and puts the total in `X-Total-Count`, so existing clients keep working.
- The rate limiter is in memory and keyed on the connection address, not on `X-Forwarded-For` (which a client can forge). Cost: per instance and reset on restart.
- One index I designed was not used by PostgreSQL on the measured data. I recorded that instead of claiming it helped.

## Blockers / risks
- No hosted deployment: the demo accounts share a documented password, so I provide a Docker run guide instead.
- Known gaps: no HTTPS in the app, no token revocation, in-memory rate limiter, offset paging on deep pages, no concurrency tests. All are listed in `docs/security-notes.md` and `docs/test-coverage.md`.

## AI and sources used
(Use your own wording from `docs/declaration.md`.) Claude was my main tool as a mentor and pair programmer; other tools are named in the declaration. References: OWASP API Security Top 10 (2023) and the Spring Security and PostgreSQL documentation. Every milestone was verified with my own command output.