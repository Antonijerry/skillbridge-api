# W2-6 verification: opportunities, requirements, lifecycle. Dummy accounts only. Prints no tokens.
$base = "http://localhost:8080"

$files = [ordered]@{
    "opp-ok.json"         = '{"title":"Backend Engineer, Payments Team","description":"Internal move to the payments team, building Spring Boot services.","closingDate":"2030-12-31"}'
    "opp-bad.json"        = '{"title":"x","description":"short"}'
    "opp-past.json"       = '{"title":"Past closing date","description":"This opportunity closed long ago.","closingDate":"2020-01-01"}'
    "req-java.json"       = '{"skillId":"aaaaaaaa-0000-0000-0000-000000000001","minimumLevel":"INTERMEDIATE"}'
    "req-sql.json"        = '{"skillId":"aaaaaaaa-0000-0000-0000-000000000002","minimumLevel":"BEGINNER"}'
    "req-unknown.json"    = '{"skillId":"99999999-9999-9999-9999-999999999999","minimumLevel":"BEGINNER"}'
    "req-bad-level.json"  = '{"skillId":"aaaaaaaa-0000-0000-0000-000000000001","minimumLevel":"GURU"}'
    "register-three.json" = '{"fullName":"Test User Three","email":"test.three@example.com","password":"Str0ng-Pass-789"}'
    "login-three.json"    = '{"email":"test.three@example.com","password":"Str0ng-Pass-789"}'
}
foreach ($name in $files.Keys) { [IO.File]::WriteAllText((Join-Path $PSScriptRoot $name), $files[$name]) }

function BuildArgs($method, $token, $file) {
    $a = @("-s", "-X", $method)
    if ($token) { $a += @("-H", "Authorization: Bearer $token") }
    if ($file)  { $a += @("-H", "Content-Type: application/json", "-d", "@requests/$file") }
    return $a
}
function Show($label, $method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a -w "`n${label}: %{http_code}`n" "$base$path"
}
function Json($method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a "$base$path" | ConvertFrom-Json
}
function Sql($query) {
    docker compose exec db psql -U skillbridge -d skillbridge -c $query
}

"waiting for the app..."
do { Start-Sleep -Seconds 1; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skill-assessments" } while ($up -ne "401")
"app is up"

curl.exe -s -o NUL -X POST "$base/api/auth/register" -H "Content-Type: application/json" -d "@requests/register-three.json"
Sql "DELETE FROM opportunities WHERE created_by IN (SELECT id FROM employees WHERE email LIKE 'test.%'); UPDATE skills SET active=true; UPDATE employees SET active=true WHERE email LIKE 'test.%'; UPDATE employees SET role='EMPLOYEE' WHERE email='test.one@example.com'; UPDATE employees SET role='MANAGER' WHERE email IN ('test.two@example.com','test.three@example.com');"

$t1 = (Json POST $null "/api/auth/login" "login-ok.json").accessToken
$t2 = (Json POST $null "/api/auth/login" "login-two.json").accessToken
$t3 = (Json POST $null "/api/auth/login" "login-three.json").accessToken
if (-not $t1 -or -not $t2 -or -not $t3) { "STOP: login failed"; return }
"logins ok (one = EMPLOYEE, two = MANAGER and creator, three = MANAGER)"

"--- 1: create ---"
Show "employee creates (expect 403)" POST $t1 "/api/opportunities" "opp-ok.json"
Show "bad input (expect 400)" POST $t2 "/api/opportunities" "opp-bad.json"
$resp = curl.exe -s -i -X POST -H "Authorization: Bearer $t2" -H "Content-Type: application/json" -d "@requests/opp-ok.json" "$base/api/opportunities"
$resp
$oppA = [regex]::Match(($resp -join "`n"), "Location: /api/opportunities/([0-9a-f-]{36})").Groups[1].Value
$oppB = (Json POST $t2 "/api/opportunities" "opp-ok.json").id
$oppC = (Json POST $t2 "/api/opportunities" "opp-past.json").id
if (-not $oppA -or -not $oppB -or -not $oppC) { "STOP: could not create the opportunities"; return }
"created A (will be published), B (no requirements), C (past closing date)"

"--- 2: reading drafts ---"
Show "manager reads own draft (expect 200)" GET $t2 "/api/opportunities/$oppA" $null
Show "employee reads draft (expect 403)" GET $t1 "/api/opportunities/$oppA" $null
Show "employee list, nothing published yet (expect 200 [])" GET $t1 "/api/opportunities" $null
Show "employee asks for DRAFT (expect 200 [])" GET $t1 "/api/opportunities?status=DRAFT" $null
Show "manager list (expect 200, 3 rows)" GET $t2 "/api/opportunities" $null
Show "bad status filter (expect 400)" GET $t2 "/api/opportunities?status=bogus" $null
Show "malformed id (expect 400)" GET $t2 "/api/opportunities/not-a-uuid" $null
Show "unknown id (expect 404)" GET $t2 "/api/opportunities/99999999-9999-9999-9999-999999999999" $null
Show "no token (expect 401)" GET $null "/api/opportunities" $null

"--- 3: requirements on A ---"
Show "employee adds (expect 403)" POST $t1 "/api/opportunities/$oppA/requirements" "req-java.json"
Show "other manager adds (expect 403)" POST $t3 "/api/opportunities/$oppA/requirements" "req-java.json"
Show "creator adds Java (expect 201)" POST $t2 "/api/opportunities/$oppA/requirements" "req-java.json"
Show "same skill again (expect 409 REQUIREMENT_EXISTS)" POST $t2 "/api/opportunities/$oppA/requirements" "req-java.json"
Show "unknown skill (expect 404)" POST $t2 "/api/opportunities/$oppA/requirements" "req-unknown.json"
Show "bad level (expect 400)" POST $t2 "/api/opportunities/$oppA/requirements" "req-bad-level.json"
Sql "UPDATE skills SET active=false WHERE name='SQL';"
Show "inactive skill (expect 409 SKILL_INACTIVE)" POST $t2 "/api/opportunities/$oppA/requirements" "req-sql.json"
Sql "UPDATE skills SET active=true WHERE name='SQL';"

"--- 4: publish rules ---"
$null = Json POST $t2 "/api/opportunities/$oppC/requirements" "req-java.json"
Show "other manager publishes (expect 403)" PATCH $t3 "/api/opportunities/$oppA/publish" $null
Show "publish with no requirements (expect 409)" PATCH $t2 "/api/opportunities/$oppB/publish" $null
Show "publish with past closing date (expect 409)" PATCH $t2 "/api/opportunities/$oppC/publish" $null
Show "publish A (expect 200 PUBLISHED)" PATCH $t2 "/api/opportunities/$oppA/publish" $null

"--- 5: a published opportunity is frozen ---"
Show "edit published (expect 409)" PUT $t2 "/api/opportunities/$oppA" "opp-ok.json"
Show "add requirement to published (expect 409)" POST $t2 "/api/opportunities/$oppA/requirements" "req-sql.json"
Show "delete published (expect 409)" DELETE $t2 "/api/opportunities/$oppA" $null
Show "publish again (expect 409)" PATCH $t2 "/api/opportunities/$oppA/publish" $null
Show "employee reads published (expect 200, requirement with skillName)" GET $t1 "/api/opportunities/$oppA" $null
Show "employee list (expect 200, one row)" GET $t1 "/api/opportunities" $null
Show "employee asks for CLOSED (expect 200 [])" GET $t1 "/api/opportunities?status=CLOSED" $null
Show "employee reads a draft (expect 403)" GET $t1 "/api/opportunities/$oppB" $null

"--- 6: close ---"
Show "close (expect 200 CLOSED)" PATCH $t2 "/api/opportunities/$oppA/close" $null
Show "close again (expect 409)" PATCH $t2 "/api/opportunities/$oppA/close" $null
Show "employee reads closed (expect 403)" GET $t1 "/api/opportunities/$oppA" $null

"--- 7: admin and delete ---"
Show "other manager edits draft B (expect 403)" PUT $t3 "/api/opportunities/$oppB" "opp-ok.json"
Sql "UPDATE employees SET role='ADMIN' WHERE email='test.three@example.com';"
Show "same user, now ADMIN, edits B (expect 200)" PUT $t3 "/api/opportunities/$oppB" "opp-ok.json"
Show "admin deletes B (expect 204)" DELETE $t3 "/api/opportunities/$oppB" $null
Show "B is gone (expect 404)" GET $t2 "/api/opportunities/$oppB" $null
$rid = (Json GET $t2 "/api/opportunities/$oppC" $null).requirements[0].id
Show "remove requirement from C (expect 204)" DELETE $t2 "/api/opportunities/$oppC/requirements/$rid" $null
Show "remove it again (expect 404)" DELETE $t2 "/api/opportunities/$oppC/requirements/$rid" $null
$null = Json POST $t2 "/api/opportunities/$oppC/requirements" "req-java.json"
Show "creator deletes draft C with a requirement (expect 204)" DELETE $t2 "/api/opportunities/$oppC" $null
Sql "SELECT count(*) AS requirements_left_for_C FROM opportunity_requirements WHERE opportunity_id = '$oppC';"
"--- done ---"