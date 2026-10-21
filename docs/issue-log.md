# Issue log

Bugs and problems found while building SkillBridge API, the options considered and how each was fixed.
Entries 1 to 8 were found during Week 1. Newer entries are added at the bottom as they happen.

| # | Found | Problem | Options considered | Decision and fix | Evidence |
|---|---|---|---|---|---|
| 1 | Week 1, audit of the starter repo | README contained placeholders (`YOUR-USERNAME`, `YOUR-FOLDER-ID`) and draft comments, so a peer copying the clone command would get a 404 | Leave until later; fix now | Fixed before any other work, because peers score the README from a clean clone | commit b1493b6 |
| 2 | Week 1, audit | README promised `DB_PASSWORD=local_dev_password` and `DB_PORT=5433`, but `.env.example` said `change_me` and `5432` | Change the README; change `.env.example` | Changed `.env.example`, since 5433 avoids a clash with a local PostgreSQL on 5432 | commit b1493b6 |
| 3 | Week 1, schema review | The `UNIQUE (employee_id, skill_id)` constraint allowed one assessment per skill, so a rejected employee could never resubmit and a verified level could not be upgraded | Reset a rejected row to PENDING on edit; delete-and-recreate; keep history with one open claim | Kept history with a partial unique index (one PENDING per employee and skill). Added as migration V2 instead of editing the applied V1 | V2 applied on a fresh database |
| 4 | Week 1, V2 verification | `docker compose up` failed with `Bind for 0.0.0.0:8080 failed: port is already allocated` and the app container never started | Kill the process; change the app port; stop the old stack | A leftover container from the clean-clone test held the port. Found with `docker ps -a` and removed with `docker compose down -v` in that folder. README troubleshooting row added | docker ps output |
| 5 | Week 1, pasting git output | The git pager (`less`) swallowed pasted keystrokes and saved a 20 KB file with a garbled name into the repo folder | Delete the file by hand; stop the pager from opening | Confirmed the file was only a saved `git diff`, deleted it, and set `git config --global core.pager cat` | git status clean |
| 6 | Week 1, copying commands | Copying command lines together with the shell prompt made `cmd` treat `>` as output redirection and created empty files named `cd` and `docker` | Ignore them; delete them | Deleted them before tagging. Rule adopted: copy only the command, never the prompt | git status clean |
| 7 | Week 1, clean-clone test | A test was run from the wrong folder and against an old database, so it looked like a pass but proved nothing about a fresh clone | Accept the "Started" line; redo the test | Redid it in the clone with a fresh volume and checked for `Schema history table ... does not exist yet` and `Successfully applied 3 migrations` | Flyway log lines |
| 8 | Week 1, pre-tag review | README listed `GET /api/skills`, which was not built, and one sentence contained a literal `...` left from instructions. The work plan also promised that endpoint and `.http` files that did not exist | Build the endpoint now; remove it from the docs | Deferred `GET /api/skills` to Week 2 so every built endpoint matches the spec, and corrected the README, work plan and spec | commits 419a345 and 361dbf3 |

## Known limitations carried into Week 2
- No authentication: `employeeId` is sent in the request body. Week 2 replaces it with the id from the login token.
- Assessments reference employees and skills through plain UUID columns. Week 2 converts them to JPA relationships.
- Withdrawing a PENDING claim deletes it permanently, so withdrawn claims leave no history.
