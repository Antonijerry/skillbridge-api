# W2-5 verification: skills, skillName, employee endpoints. Dummy accounts only. Prints no tokens.
$base = "http://localhost:8080"

"waiting for the app..."
do { Start-Sleep -Seconds 1; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skill-assessments" } while ($up -ne "401")
"app is up"

docker compose exec db psql -U skillbridge -d skillbridge -c "DELETE FROM skill_assessments WHERE employee_id IN (SELECT id FROM employees WHERE email LIKE 'test.%'); DELETE FROM skills WHERE name = 'Kubernetes'; UPDATE employees SET role='EMPLOYEE', active=true WHERE email='test.one@example.com'; UPDATE employees SET role='ADMIN', active=true WHERE email='test.two@example.com';"

$t1 = (curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@requests/login-ok.json" | ConvertFrom-Json).accessToken
$t2 = (curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@requests/login-two.json" | ConvertFrom-Json).accessToken
if (-not $t1 -or -not $t2) { "STOP: login failed"; return }
"logins ok (test.one = EMPLOYEE, test.two = ADMIN)"

"--- 1: me (expect 200 twice, no password field) ---"
curl.exe -s -w "`nme employee: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/employees/me"
curl.exe -s -w "`nme admin: %{http_code}`n" -H "Authorization: Bearer $t2" "$base/api/employees/me"
$id1 = (curl.exe -s -H "Authorization: Bearer $t1" "$base/api/employees/me" | ConvertFrom-Json).id
$id2 = (curl.exe -s -H "Authorization: Bearer $t2" "$base/api/employees/me" | ConvertFrom-Json).id
if (-not $id1 -or -not $id2) { "STOP: me failed"; return }

"--- 2: skills (expect 200, 200 [], 400, 200, 404, 400, 401) ---"
curl.exe -s -w "`nlist: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills"
curl.exe -s -w "`ninactive only: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills?active=false"
curl.exe -s -w "`nbad filter: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills?active=maybe"
curl.exe -s -w "`none: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills/aaaaaaaa-0000-0000-0000-000000000001"
curl.exe -s -w "`nunknown id: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills/99999999-9999-9999-9999-999999999999"
curl.exe -s -w "`nmalformed id: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills/not-a-uuid"
curl.exe -s -w "`nno token: %{http_code}`n" "$base/api/skills"

"--- 3: add skills (expect 403, 201 + Location, 409, 409, 400) ---"
curl.exe -s -w "`nemployee adds: %{http_code}`n" -X POST -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "@requests/skill-ok.json" "$base/api/skills"
curl.exe -s -i -X POST -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/skill-ok.json" "$base/api/skills"
curl.exe -s -w "`nsame name again: %{http_code}`n" -X POST -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/skill-ok.json" "$base/api/skills"
curl.exe -s -w "`nsame name, other case: %{http_code}`n" -X POST -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/skill-dup-case.json" "$base/api/skills"
curl.exe -s -w "`nbad input: %{http_code}`n" -X POST -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/skill-bad.json" "$base/api/skills"

"--- 4: assessments carry skillName ---"
$x = curl.exe -s -X POST "$base/api/skill-assessments" -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "@requests/assessment-create.json" | ConvertFrom-Json
"created skillName: " + $x.skillName + " / status: " + $x.status
if (-not $x.id) { "STOP: could not create the claim"; return }
curl.exe -s -w "`nlist own: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skill-assessments"

"--- 5: an employee's assessments (expect 200, 200, 403, 404, 400, 200 [], 400, 401) ---"
curl.exe -s -w "`nown: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/employees/$id1/skill-assessments"
curl.exe -s -w "`nadmin views employee: %{http_code}`n" -H "Authorization: Bearer $t2" "$base/api/employees/$id1/skill-assessments"
curl.exe -s -w "`nemployee views admin: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/employees/$id2/skill-assessments"
curl.exe -s -w "`nunknown employee: %{http_code}`n" -H "Authorization: Bearer $t2" "$base/api/employees/99999999-9999-9999-9999-999999999999/skill-assessments"
curl.exe -s -w "`nmalformed id: %{http_code}`n" -H "Authorization: Bearer $t2" "$base/api/employees/not-a-uuid/skill-assessments"
curl.exe -s -w "`nstatus VERIFIED filter: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/employees/$id1/skill-assessments?status=VERIFIED"
curl.exe -s -w "`nbad status: %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/employees/$id1/skill-assessments?status=bogus"
curl.exe -s -w "`nno token: %{http_code}`n" "$base/api/employees/$id1/skill-assessments"
"--- done ---"