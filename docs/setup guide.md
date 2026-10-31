# Setup guide

How to run SkillBridge API from a clean machine, run the tests and reset everything. Written for someone who has never seen the project. The project runs entirely locally: no cloud account, API key or third-party service is needed.

## 1. What you need
| Need | Why | Check |
|---|---|---|
| Git | to clone the repository | `git --version` |
| Docker Desktop, **running** | runs the database and (for Option A) the application, and the integration tests | `docker version` shows a Client **and** a Server section |
| JDK 21 (Option B and the tests only) | to build and test without Docker for the app | `java -version` shows 21 |
| Free ports 8080 and 5433 | the API and the database | see Troubleshooting if they are taken |

No Maven install is needed: the repository ships the Maven wrapper (`mvnw`, `mvnw.cmd`).

## 2. Get the code
```bash
git clone --branch week-3 https://github.com/Antonijerry/skillbridge-api.git
cd skillbridge-api
```
The tags `week-1`, `week-2` and `week-3` are snapshots of each week.

## 3. Create your local configuration
```bash
cp .env.example .env        # Windows: copy .env.example .env
```
`.env` is ignored by Git and holds your local values. The database values in it are dummy local values and work as they are. **You must set `JWT_SECRET`**: the application refuses to start while it is the placeholder `change-me` or shorter than 32 bytes.

```powershell
# Windows PowerShell: generate a random key and write it into .env
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$secret = ($b | ForEach-Object { $_.ToString("x2") }) -join ""
(Get-Content .env) -replace '^JWT_SECRET=.*', "JWT_SECRET=$secret" | Set-Content .env
```
```bash
# macOS or Linux
sed -i "s/^JWT_SECRET=.*/JWT_SECRET=$(openssl rand -hex 32)/" .env
```
Never commit `.env` or paste the key anywhere public.

## 4. Run it

### Option A: everything in Docker (recommended)
```bash
docker compose up --build
```
The first build downloads images and dependencies and can take several minutes with little output. Wait for `Started SkillbridgeApiApplication`. The API is then at **http://localhost:8080**.

### Option B: the database in Docker, the application from source
```bash
docker compose up -d db
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

On first start, Flyway applies migrations V1 to V6: it creates the tables and indexes and seeds three demo accounts, six skills and two sample assessments.

## 5. Check that it works
1. Log in as the demo employee (Windows PowerShell; use `curl` on macOS or Linux):
   ```powershell
   curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "@requests/login-ada.json"
   ```
   Expected: JSON with `accessToken`, `"tokenType":"Bearer"` and `expiresInSeconds`.
2. Call a protected endpoint without a token: `curl.exe -i http://localhost:8080/api/skills`. Expected: `401` and `{"error":{"code":"UNAUTHORIZED", ...}}`.
3. Run the 34-check story end to end (needs a fresh database):
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\requests\w2-9-smoke.ps1
   ```
   Expected last line: `ALL 34 CHECKS PASSED`.
   Or import `postman/skillbridge-api.postman_collection.json` into Postman and run the collection.

Demo accounts (dummy data, password `Demo-Pass-123` for all): `ada@skillbridge.local` (EMPLOYEE), `tunde@skillbridge.local` (MANAGER), `chidi@skillbridge.local` (ADMIN).

### Try the API in the browser (Swagger UI)
1. With the API running, open **http://localhost:8080/swagger-ui.html**.
2. Open `POST /api/auth/login`, click **Try it out**, use `ada@skillbridge.local` and the demo password `Demo-Pass-123` (it is not in `.env`), then **Execute**. Copy `accessToken` from the response.
3. Click **Authorize** at the top, paste the token (without the word `Bearer`), then **Authorize** and **Close**.
4. Open `GET /api/skills`, **Try it out**, **Execute**. Expected: `200`, a JSON array, and the header `x-total-count`.
5. Without authorizing, the same call returns `401 UNAUTHORIZED`. That is the expected behaviour, not a fault.

The raw OpenAPI document is at http://localhost:8080/v3/api-docs. To switch both off (recommended for any real deployment), set `SWAGGER_ENABLED=false`.

## 6. Run the tests
```powershell
.\mvnw.cmd test            # macOS or Linux: ./mvnw test
```
- Docker must be running; the integration tests start their own PostgreSQL 16 container, so you do **not** need the application or the `db` container running, and your development data is never touched.
- A full run takes about 2 to 3 minutes; the first run also downloads the `postgres:16` image.
- Expected: `Tests run: 97, Failures: 0, Errors: 0` and `BUILD SUCCESS`.
- A coverage report is written to `target/site/jacoco/index.html`.
- Unit tests only (seconds, no Docker): `.\mvnw.cmd test "-Dtest=MatchServiceTest,ApplicationServiceTest,ClaimedLevelTest,RegisterRequestTest,PageParamsTest,AuthRateLimitFilterTest"`

## 7. Optional settings
| Variable | Meaning | Default |
|---|---|---|
| `JWT_EXPIRY_MINUTES` | Token lifetime | `60` |
| `RATE_LIMIT_AUTH_MAX` | Register and login requests per window and client address | `20` |
| `RATE_LIMIT_AUTH_WINDOW_SECONDS` | Window length | `60` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API | none |
| `SWAGGER_ENABLED` | Serve Swagger UI and `/v3/api-docs` | `true` |

Put them in `.env`. When running from source (Option B) they are picked up from there. For Option A, Docker Compose only passes the variables that `docker-compose.yml` lists, so add the ones you need under `app:` → `environment:`, after the `JWT_SECRET` line (the project's compose file has `DB_*` and `JWT_SECRET` only):
```yaml
      JWT_EXPIRY_MINUTES: ${JWT_EXPIRY_MINUTES:-60}
      RATE_LIMIT_AUTH_MAX: ${RATE_LIMIT_AUTH_MAX:-20}
      RATE_LIMIT_AUTH_WINDOW_SECONDS: ${RATE_LIMIT_AUTH_WINDOW_SECONDS:-60}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-}
      SWAGGER_ENABLED: ${SWAGGER_ENABLED:-true}
```
If the smoke script or Postman collection reports `429`, you sent more than 20 register or login requests in a minute: wait for the `Retry-After` seconds, or set `RATE_LIMIT_AUTH_MAX=1000` for local runs.

## 8. Stop and reset
```bash
docker compose down        # stop; keeps the database data
docker compose down -v     # stop and DELETE the local database (all data you created)
```
Reset with `-v` before re-running the smoke script or the Postman collection, because both assume a fresh database.

## 9. Troubleshooting
| Symptom | Cause and fix |
|---|---|
| The app exits at startup mentioning `skillbridge.jwt.secret` or a key that is too short | `JWT_SECRET` in `.env` is missing, still `change-me`, or under 32 bytes. Generate one (step 3) |
| `Cannot resolve placeholder 'DB_USER'` (Option B) | `.env` does not exist in the repository root. Create it (step 3) |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | Another program or an older copy of this project uses port 8080. Run `docker compose down` in the old copy or stop the program (`netstat -ano \| findstr :8080` on Windows) |
| `port is already allocated` for the database | Port 5433 is taken. Set `DB_PORT` in `.env` to a free port, then `docker compose down` and start again |
| `password authentication failed` | `DB_PASSWORD` was changed after the first start (run `docker compose down -v`), or another PostgreSQL uses your `DB_PORT` |
| `Could not find a valid Docker environment` while running tests | Docker Desktop is not running. Start it and wait until it reports running |
| Docker command not found, or an engine error | Install or start Docker Desktop |
| Every request returns `401` | Tokens last 60 minutes. Log in again |
| `429 TOO_MANY_REQUESTS` | See step 7 |
| In PowerShell `curl` behaves oddly | `curl` is an alias for another command. Use `curl.exe` |
| The smoke script fails at the first claim | The database was used before. `docker compose down -v` and start again |
| The first build seems stuck | It is downloading images and dependencies. Wait; later starts take seconds |

## 10. Deployment
This project is run locally with Docker; it is not hosted. The seeded demo accounts share a documented password, so exposing this build to the internet would be unsafe. Before any real deployment: remove the demo seed (V3 and V5), terminate TLS in front of the API, use a long random `JWT_SECRET` held in a secret store, set `CORS_ALLOWED_ORIGINS` to the real front end only, and read `docs/security-notes.md` for the known gaps.