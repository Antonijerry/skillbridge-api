# Security Notes (Week 3)

What is protected, how, and how it is checked. "Test" means an automated test in `src/test` that passes in `.\mvnw.cmd test` (97 tests at the time of writing). Anything not tested says so.

## Summary

| Area | Control | Evidence |
|------|---------|----------|
| Secrets | Only in environment variables or an untracked `.env`; `.env.example` holds placeholders. The app refuses to start without `JWT_SECRET`, and the key must be at least 32 bytes | Manual: `git ls-files .env` is empty and a history search for the secret is empty (Week 2). Not automated |
| Passwords | BCrypt hash only. The password is never returned, never trimmed, never printed (`toString` hides it). Maximum 72 bytes, because BCrypt ignores anything longer | Test: request normalisation and `toString`; 400 for a password over the limit |
| Login errors | One answer (401 `INVALID_CREDENTIALS`) for an unknown email, a wrong password and an inactive account. A dummy hash is checked for unknown emails so response time does not reveal which emails exist | Test: wrong password and unknown email return the same 401. Timing equality is by design and not measured |
| Tokens | Signed JWT (HS256), 60 minutes. The client never sends its own id. Role and active status are re-read from the database on every request, so a role change or deactivation applies at once | Test: garbage token returns 401 in the standard format. Immediate deactivation is covered by the Week 2 smoke script, not by a unit test |
| Authorisation | Order: wrong role (403), then missing record (404), then wrong owner (403). A caller with no right to a resource learns nothing about whether it exists | Tests: employee on manager actions gets 403 even for a random id; another employee cannot read or withdraw my claim; a manager cannot verify their own claim; a withdraw by anyone but the applicant is 403 |
| Input validation | Bean Validation on every request body, typed ids, enums and dates, JSON only. Text is trimmed. Database CHECK constraints repeat the main limits | Tests: 400 for bad email, short or long password, bad evidence, malformed JSON, non-numeric paging, non-UUID path value |
| SQL injection | All queries are parameter bound (Spring Data and JPA Criteria); no string-built SQL | Test: `' OR '1'='1` as an email is just an unknown email (401, never 500) |
| Output / XSS | The API returns JSON with `Content-Type: application/json` and `X-Content-Type-Options: nosniff`. Text is stored as data and not altered; escaping belongs where HTML is rendered | Test: markup in a text field is stored and returned unchanged as JSON, with the nosniff header |
| Error handling | One error format. No stack traces, SQL or class names. `server.error.include-*` is `never` | Test: a bad id returns 400 without Java internals in the body |
| Rate limiting | `POST /api/auth/*` allows 20 requests per 60 seconds per client address (configurable); then 429 `TOO_MANY_REQUESTS` with `Retry-After` | Tests: unit tests of the limiter (limit, reset, per client, other routes unaffected) and one end-to-end 429 test |
| CORS | Only origins in `CORS_ALLOWED_ORIGINS` may call the API from a browser (default: none). No credentials. Exposes `X-Total-Count` and `Retry-After` only | Tests: allowed origin accepted, other origin rejected, default rejects every origin, exposed header present |
| Resource limits | Page size capped at 100; `size` and `page` validated | Tests: 400 for size 0 or 101 and negative page |
| Server-side requests | `evidenceUrl` is only stored, never fetched, so the API cannot be used to reach internal addresses (SSRF) | By design; nothing in the code makes outgoing requests |

## Mapping to the OWASP API Security Top 10 (2023)

| Risk | Position |
|------|----------|
| API1 Broken object level authorisation | Ownership is checked on every object endpoint (claims, applications, opportunities); tested |
| API2 Broken authentication | BCrypt, uniform login error, signed expiring tokens, rate limit on login and register. **No account lockout, no multi-factor, no token revocation list** |
| API3 Broken object property level authorisation | Responses are DTOs, never entities; requests are records with only the allowed fields, so a client cannot set `role`, `status` or `employeeId` through a body |
| API4 Unrestricted resource consumption | Pagination cap and auth rate limit. **No request body size limit beyond the server default, no global rate limit** |
| API5 Broken function level authorisation | Role checks in the services (verify, reject, publish, role change, skill creation are staff or admin only); tested for the main ones |
| API6 Sensitive business flows | One application per employee per opportunity (database constraint); one pending claim per skill. No other abuse controls |
| API7 SSRF | Not applicable: no outgoing requests |
| API8 Security misconfiguration | Error detail off, CORS explicit, CSRF off only because there are no cookies or sessions. **HTTPS is not enforced by the app** (see below) |
| API9 Improper inventory | `docs/api-spec.md` and the Postman collection list every endpoint |
| API10 Unsafe consumption of APIs | Not applicable: no third-party APIs are called |

## Decisions and trade-offs

- **Rate limiter in memory, no library.** It adds no dependency and is easy to test. The cost: it is per instance, resets on restart, uses a fixed window (a client can send a burst at a window boundary) and is not shared between several instances. A gateway, a shared store or a library would be the next step.
- **Client address, not `X-Forwarded-For`.** The header can be forged by any client, so trusting it would let an attacker dodge the limit. The cost: behind a reverse proxy every user shares the proxy's address until forwarded headers are configured for a trusted proxy only.
- **Role read from the database on every request**, not trusted from the token. One extra query per request buys instant role changes and instant deactivation.
- **CORS closed by default.** An empty origin list means no browser on another origin can call the API. Setting `*` is possible but not recommended.
- **Swagger UI and `/v3/api-docs` are public (no token) and on by default.** A reviewer or new developer can see every endpoint without an account, which is useful locally and also a map for an attacker. Set `SWAGGER_ENABLED=false` for a real deployment. Trying a protected endpoint from the UI still needs a valid token; the UI does not bypass any check.
- **No HTML stripping.** Removing markup on input would corrupt legitimate text and give a false sense of safety. The API is JSON only, and the client must escape when it renders.

## Known gaps (stated, not hidden)

- **No HTTPS in the application.** Run it behind a reverse proxy or platform that terminates TLS, and add HSTS there. Tokens sent over plain HTTP can be stolen.
- **No token revocation or refresh.** A stolen token works until it expires (60 minutes) or the account is deactivated.
- **The HS256 secret is shared and symmetric.** Anyone who gets `JWT_SECRET` can mint tokens. It must be long, random and rotated if exposed. There is no key rotation support.
- **Demo accounts use a documented password** (`Demo-Pass-123`). They exist for local demos and must be removed before a real deployment.
- **No dependency vulnerability scan was run.**
- **No log audit** beyond making sure request objects do not print passwords.
- **Tomcat's own HTML 400 for invalid percent-encoding** in a URL falls outside the error format (logged in the issue log).
- **No penetration test** was done; the controls above are checked by the automated tests listed.

## References
Read these yourself before relying on them; they are the sources the design follows.
- OWASP API Security Top 10 (2023): https://owasp.org/API-Security/editions/2023/en/0x11-t10/
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- OWASP JSON Web Token Cheat Sheet for Java: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
- Spring Security reference documentation: https://docs.spring.io/spring-security/reference/