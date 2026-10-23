# Week 2 update: SkillBridge API

**Author:** Anthony Jerry Kalu · **Date:** 23 October 2026 · **Tag:** `week-2` · **Branch:** `week-2`
**Repository:** https://github.com/Antonijerry/skillbridge-api

## What I built
- Registration and login with JWT. Passwords are stored as BCrypt hashes and never logged. Identity comes only from the token, never from the request body.
- Three roles (EMPLOYEE, MANAGER, ADMIN), ownership checks, and admin endpoints to change a role or deactivate an account. A deactivated account's token stops working immediately because the account is re-read on every request.
- A skills catalogue, manager review of skill claims (verify or reject), opportunities with required skills and a DRAFT, PUBLISHED, CLOSED lifecycle, a match percentage computed from verified skills only, and applications with shortlist, reject and withdraw.
- One error format for every failure, validation on every input, and a global exception handler.
- Demo accounts and a ready-made walkthrough (a Postman collection and a PowerShell smoke test) so a reviewer can run the whole story.

## How I verified it
Each milestone has a PowerShell script in `requests/` that prints a labelled result next to the expected status, and I pasted the real output before closing each milestone. Highlights:
- 21 input-limit cases all return 400, including a password that is 40 characters but 80 bytes (BCrypt's limit is 72 bytes).
- 16 admin and deactivation steps: employees and managers get 403, an admin cannot demote or deactivate themselves (409), and a deactivated user's old token returns 401 and works again after reactivation.
- The match for a test employee is 2 of 3 requirements, 66 percent (rounded down). A pending claim and a rejected claim are ignored, and a verified level below the minimum does not count.
- The smoke test (`requests/w2-9-smoke.ps1`) prints `ALL 34 CHECKS PASSED`. The Postman collection ran green: 35 of 35 checks (34 requests plus an extra check on the match numbers).

## Problems I hit and how I solved them
The full list (entries 9 to 20) is in `docs/issue-log.md`. Examples:
- Spring Security created a default user with a random password printed in the log. I defined my own `UserDetailsService` so it backs off.
- A stale Week 1 container held port 8080 and answered every request, so my new code seemed to be ignored.
- `POST .../requirements` returned `"id": null`: `save()` on an already-managed entity merged a copy. Switching to `flush()` fixed it.
- The demo password hash lost its last character when I pasted it (59 characters instead of 60), so no demo login worked. I found it by checking the hash length and verifying it in the database.
- Windows PowerShell has reserved variables (`$pid`) and returns JSON arrays as one object. Both broke my check scripts until I found them.

## Known limitations
Two reviewers acting on the same claim at the same instant: the last write wins. Lists are not paginated. A withdrawn application cannot be re-submitted. A URL with invalid percent-encoding returns the server's plain HTML 400. The seeded demo accounts share a known password and must not ship to a real deployment. The rest are in the issue log.

## Feedback
No Week 1 feedback had arrived when I wrote this. See `docs/feedback-response.md`.

## Next (Week 3)
Automated unit and integration tests, OpenAPI documentation, pagination, security and performance notes, a deployment or setup guide, the walkthrough video and the reflection.

## Use of AI (Week 2)
I used three AI tools in Week 2: Claude (Anthropic), ChatGPT and Gemini.

- **Claude** was my mentor and pair programmer for the Week 2 plan and its first milestones. It wrote the milestone plan that maps each requirement in the BuildLab guide to a milestone (`docs/week-2-milestones.md`), drafted the issue log from the problems I hit in Week 1, and drafted the specification for registration, login and the new 401/403 error codes. It also drafted the JWT security configuration, the registration and login code, the updated global error handler, the request bodies and the Postman collections, and it explained the Spring Boot 4 package change that broke my `ErrorController` import. It reviewed the output I pasted back and pointed out mismatches, such as differences between the login response field names and the spec, and a MANAGER account that registration should not be able to create.
- **ChatGPT:** [state exactly what it did in Week 2, for example "explained X", "drafted Y", or "not used in Week 2"].
- **Gemini:** [state exactly what it did in Week 2, or "not used in Week 2"].

I made the design decisions, applied every change myself, ran every command and pasted the real output back for each milestone, and debugged the failures that came up (for example [the truncated demo hash and the stale container, if those are real]). Nothing was marked verified without my own command output. [Name anything I wrote or changed by hand.]