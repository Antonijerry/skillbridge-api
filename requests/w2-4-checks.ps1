# W2-4 verification: verify and reject. Dummy test accounts and files in requests\ only. Prints no tokens.
$base = "http://localhost:8080"

"waiting for the app..."
do { Start-Sleep -Seconds 1; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skill-assessments" } while ($up -ne "401")
"app is up"

docker compose exec db psql -U skillbridge -d skillbridge -c "DELETE FROM skill_assessments WHERE employee_id IN (SELECT id FROM employees WHERE email LIKE 'test.%'); UPDATE employees SET role='EMPLOYEE', active=true WHERE email='test.one@example.com'; UPDATE employees SET role='MANAGER', active=true WHERE email='test.two@example.com';"

$t1 = (curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@requests/login-ok.json" | ConvertFrom-Json).accessToken
$t2 = (curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@requests/login-two.json" | ConvertFrom-Json).accessToken
if (-not $t1 -or -not $t2) { "STOP: login failed"; return }
"logins ok"

"--- 1: create claims (employee x, manager y) ---"
$x = (curl.exe -s -X POST "$base/api/skill-assessments" -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "@requests/assessment-create.json" | ConvertFrom-Json).id
$y = (curl.exe -s -X POST "$base/api/skill-assessments" -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/assessment-create.json" | ConvertFrom-Json).id
if (-not $x -or -not $y) { "STOP: could not create the claims (x=$x y=$y)"; return }
"claims ok"

"--- 2: who is refused (expect 403, 403, 404, 401) ---"
curl.exe -s -w "`nemployee verify: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t1" "$base/api/skill-assessments/$x/verify"
curl.exe -s -w "`nmanager own claim: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" "$base/api/skill-assessments/$y/verify"
curl.exe -s -w "`nunknown id: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" "$base/api/skill-assessments/99999999-9999-9999-9999-999999999999/verify"
curl.exe -s -w "`nno token: %{http_code}`n" -X PATCH "$base/api/skill-assessments/$x/verify"

"--- 3: verify, verify again, owner edits a verified claim (expect 200, 409, 409) ---"
curl.exe -s -w "`nverify: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/verify-ok.json" "$base/api/skill-assessments/$x/verify"
curl.exe -s -w "`nverify again: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" "$base/api/skill-assessments/$x/verify"
curl.exe -s -w "`nowner edits verified: %{http_code}`n" -X PUT -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "@requests/assessment-update.json" "$base/api/skill-assessments/$x"

"--- 4: reject (expect 400, then 200) ---"
$z = (curl.exe -s -X POST "$base/api/skill-assessments" -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "@requests/assessment-create-2.json" | ConvertFrom-Json).id
if (-not $z) { "STOP: could not create the second claim"; return }
curl.exe -s -w "`nreject no comment: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/reject-bad.json" "$base/api/skill-assessments/$z/reject"
curl.exe -s -w "`nreject: %{http_code}`n" -X PATCH -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/reject-ok.json" "$base/api/skill-assessments/$z/reject"
"--- done ---"