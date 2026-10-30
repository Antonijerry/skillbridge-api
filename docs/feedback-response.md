# Response to Week 1 Feedback

## Week 1 result
Week 1 scores arrived after the Week 2 work was done. No written comments were included, so this response is based on the scores only.

| Criterion | Mentor | Peer |
|-----------|--------|------|
| Data model, API and business-rule design | 4/5 | 4/5 |
| Validation, authentication and security approach | 3/5 | 4/5 |
| Reliability, testing and operational readiness | 4/5 | 4/5 |
| Evidence gathering and synthesis | 4/5 | 2/5 |
| Assumptions, trade-offs and decision rationale | 3/5 | 4/5 |
| Completeness, organisation and reliability | 4/5 | 4/5 |
| Testing, review and iteration | 3/5 | 4/5 |
| Documentation and walkthrough | 3/5 | 4/5 |
| Learning, limitations and next-step judgement | 4/5 | 5/5 |

## Changes made in Week 2 on my own initiative
| Change | Outcome | Evidence |
|--------|---------|----------|
| employeeId taken from the token, never the request body | Done in W2-3 | docs/issue-log.md, entry 13 |
| `GET /api/skills` added | Done in W2-5 | docs/api-spec.md |
| JPA relationships between aggregates | Deliberately not done. Aggregates reference each other by id (design decision 20); only Opportunity owns its requirements | README, engineering decisions |

## Plan for the lowest-scoring areas (Week 3)
- **Security approach (mentor 3/5):** security tests for 400/401/403, rate limiting on auth routes, deliberate CORS settings, written security notes.
- **Decision rationale (mentor 3/5):** the case study will state, for each key decision, the alternative I rejected and why.
- **Testing, review and iteration (mentor 3/5):** automated unit and integration tests, plus a coverage note stating what is not tested and why.
- **Documentation and walkthrough (mentor 3/5):** setup guide, documented endpoints with examples, a rehearsed walkthrough.
- **Evidence gathering (peer 2/5):** sources behind decisions will be cited in the docs and the technical reflection.