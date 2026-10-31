# AI usage log

A running record of how AI tools were used. The declaration (`docs/declaration.md`) summarises it.

## Rules I followed
- I ran every command myself and pasted the real output back before a milestone was marked verified. Nothing was marked done on the AI's word.
- No secrets, real passwords or real personal data were pasted into any tool. Test data is dummy data.
- Where an AI suggestion was wrong or incomplete, I record it below.

## Claude (Anthropic): main tool, used as a mentor and pair programmer
| Period | What it did | What I did |
|---|---|---|
| Weeks 1 and 2 | Proposed the plan and milestones; drafted the design documents, SQL migrations, Java code, check scripts and the README; explained each step | Ran every command and script, pasted the output, debugged failures, and made the decisions recorded in `docs/design-decisions.md` |
| Week 3, tests | Wrote the first drafts of the test suite (96 tests) and the Testcontainers set-up; suggested measuring coverage and which gaps mattered | Ran the suite after every step, reported failures, created the files and committed them |
| Week 3, pagination and indexes | Proposed the design (page and size, `X-Total-Count`, stable ordering, two queries for opportunities, moving filters into the database, the V6 indexes) and wrote the code and the measurement commands | Ran the code and the `EXPLAIN ANALYZE` measurements on a scratch database; the result that the composite index was not used came from my run |
| Week 3, security | Proposed and drafted the rate limiter, CORS configuration and security tests | Ran them; checked the results |
| Week 3, documents | Drafted the case-study README, setup guide, performance, security and coverage notes, issue-log entries and the update note | Checked each statement against my own output, ran the setup guide from a clean clone, and wrote the technical reflection myself |

### Where the AI was wrong or I had to correct it
- It gave a wrong test count twice (39 smoke checks instead of 34; 43 unit tests instead of 42). I corrected it from my real output.
- It suggested `git push origin week-2` while a branch and a tag shared that name. That is ambiguous; the fix was `refs/tags/week-2`.
- It first wrote the AI line of my update note as "Claude only". I corrected it because I also used other tools.
- Its first design of the composite status index was not used by PostgreSQL on the measured data (issue log entry 26).

## Other tools
<!-- Replace this block with your own entries. Keep it factual: tool, what you asked it for, what you used, what you changed. -->
Tool | What I used it for | What I used or changed
---|---|---
ChatGPT (OpenAI) | Brainstorming query strategies, validating domain logic rules, and sanity-checking API architecture decisions. | Used high-level design suggestions (e.g., approach for handling role-based application filters), but implemented the actual JPA Criteria logic and code structures manually to fit project requirements.
Gemini (Google) | Deep-diving into JPA Specification edge cases, debugging Spring Data compilation errors, and breaking down complex SQL execution plans. | Used its explanations to understand why dynamic specifications require `JpaSpecificationExecutor` and refactored specification classes to keep service methods clean and human-readable.
## Sources and references
Fill in only what you actually opened and read. Candidates used in the notes: OWASP API Security Top 10 (2023), OWASP Password Storage Cheat Sheet, OWASP JSON Web Token Cheat Sheet for Java, the Spring Security reference documentation, the PostgreSQL documentation for `EXPLAIN`.