# W2-8 verification: error-format audit, validation limits, admin account management. Dummy accounts only. Prints no tokens.
$base = "http://localhost:8080"

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
function Status($label, $method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a -o NUL -w "${label}: %{http_code}`n" "$base$path"
}
function Json($method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a "$base$path" | ConvertFrom-Json
}
function Sql($query) {
    docker compose exec db psql -U skillbridge -d skillbridge -c $query
}
function Send($label, $method, $token, $path, $obj) {
    $json = $obj | ConvertTo-Json -Compress
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot "tmp-body.json"), $json)
    Show $label $method $token $path "tmp-body.json"
}

"waiting for the app..."
do { Start-Sleep -Seconds 1; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skill-assessments" } while ($up -ne "401")
"app is up"

Sql "UPDATE skills SET active=true; UPDATE employees SET role='EMPLOYEE', active=true WHERE email='test.one@example.com'; UPDATE employees SET role='ADMIN', active=true WHERE email='test.two@example.com'; UPDATE employees SET role='MANAGER', active=true WHERE email='test.three@example.com';"

$t1 = (Json POST $null "/api/auth/login" "login-ok.json").accessToken
$t2 = (Json POST $null "/api/auth/login" "login-two.json").accessToken
$t3 = (Json POST $null "/api/auth/login" "login-three.json").accessToken
if (-not $t1 -or -not $t2 -or -not $t3) { "STOP: login failed"; return }
$id1 = (Json GET $t1 "/api/employees/me" $null).id
$id2 = (Json GET $t2 "/api/employees/me" $null).id
$id3 = (Json GET $t3 "/api/employees/me" $null).id
if (-not $id1 -or -not $id2 -or -not $id3) { "STOP: could not read the three accounts"; return }
"ready (one = EMPLOYEE, two = ADMIN, three = MANAGER)"

"--- 1: odd requests (every answer must be the one JSON error format, never HTML or a stack trace) ---"
curl.exe -s -w "`ntext/plain body (expect 415): %{http_code}`n" -X POST -H "Authorization: Bearer $t1" -H "Content-Type: text/plain" -d "hello" "$base/api/skill-assessments"
curl.exe -s -w "`nbroken JSON (expect 400): %{http_code}`n" -X POST -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "{" "$base/api/skill-assessments"
curl.exe -s -w "`nblank body (expect 400): %{http_code}`n" -X POST -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d " " "$base/api/skill-assessments"
curl.exe -s -w "`nJSON array instead of an object (expect 400): %{http_code}`n" -X POST -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "[1]" "$base/api/skill-assessments"
curl.exe -s -w "`nAccept xml (expect 406): %{http_code}`n" -H "Authorization: Bearer $t1" -H "Accept: application/xml" "$base/api/skills"
curl.exe -s -w "`nBasic auth scheme (expect 401): %{http_code}`n" -H "Authorization: Basic YWJjOmRlZg==" "$base/api/skills"
curl.exe -s -w "`nPUT on a list path (expect 405): %{http_code}`n" -X PUT -H "Authorization: Bearer $t1" -H "Content-Type: application/json" -d "{}" "$base/api/skills"
curl.exe -s -w "`nunknown path (expect 404): %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/nothing-here"
curl.exe -s -w "`nbad percent-encoding (probe, just look at the format): %{http_code}`n" -H "Authorization: Bearer $t1" "$base/api/skills/%zz"
curl.exe -s -w "`n/error endpoint (probe, just look at the format): %{http_code}`n" -H "Authorization: Bearer $t1" "$base/error"

"--- 2: validation limits (every case expects 400) ---"
$e = [string][char]0xE9
Send "register: name 1 char" POST $null "/api/auth/register" @{ fullName = "A"; email = "limit.test@example.com"; password = "Str0ng-Pass-123" }
Send "register: name 121 chars" POST $null "/api/auth/register" @{ fullName = ("a" * 121); email = "limit.test@example.com"; password = "Str0ng-Pass-123" }
Send "register: bad email" POST $null "/api/auth/register" @{ fullName = "Limit Test"; email = "not-an-email"; password = "Str0ng-Pass-123" }
Send "register: email 162 chars" POST $null "/api/auth/register" @{ fullName = "Limit Test"; email = (("a" * 150) + "@example.com"); password = "Str0ng-Pass-123" }
Send "register: password 7 chars" POST $null "/api/auth/register" @{ fullName = "Limit Test"; email = "limit.test@example.com"; password = "abcdefg" }
Send "register: password 73 chars" POST $null "/api/auth/register" @{ fullName = "Limit Test"; email = "limit.test@example.com"; password = ("a" * 73) }
Send "register: password 40 chars but 80 bytes" POST $null "/api/auth/register" @{ fullName = "Limit Test"; email = "limit.test@example.com"; password = ($e * 40) }
Send "login: password missing" POST $null "/api/auth/login" @{ email = "test.one@example.com" }
Send "skill: name 81 chars" POST $t2 "/api/skills" @{ name = ("a" * 81); category = "Test" }
Send "skill: category 61 chars" POST $t2 "/api/skills" @{ name = "Limit skill"; category = ("a" * 61) }
Send "skill: description 501 chars" POST $t2 "/api/skills" @{ name = "Limit skill"; category = "Test"; description = ("a" * 501) }
Send "opportunity: title 2 chars" POST $t2 "/api/opportunities" @{ title = "ab"; description = "A description that is long enough." }
Send "opportunity: title 151 chars" POST $t2 "/api/opportunities" @{ title = ("a" * 151); description = "A description that is long enough." }
Send "opportunity: description 2001 chars" POST $t2 "/api/opportunities" @{ title = "Valid title"; description = ("a" * 2001) }
Send "opportunity: closingDate not a date" POST $t2 "/api/opportunities" @{ title = "Valid title"; description = "A description that is long enough."; closingDate = "tomorrow" }
Send "assessment: summary 9 chars" POST $t1 "/api/skill-assessments" @{ skillId = "aaaaaaaa-0000-0000-0000-000000000004"; claimedLevel = "BEGINNER"; evidenceSummary = "too short" }
Send "assessment: summary 1001 chars" POST $t1 "/api/skill-assessments" @{ skillId = "aaaaaaaa-0000-0000-0000-000000000004"; claimedLevel = "BEGINNER"; evidenceSummary = ("a" * 1001) }
Send "assessment: url not http" POST $t1 "/api/skill-assessments" @{ skillId = "aaaaaaaa-0000-0000-0000-000000000004"; claimedLevel = "BEGINNER"; evidenceSummary = "A valid summary of the work."; evidenceUrl = "ftp://x.example" }
Send "assessment: level GURU" POST $t1 "/api/skill-assessments" @{ skillId = "aaaaaaaa-0000-0000-0000-000000000004"; claimedLevel = "GURU"; evidenceSummary = "A valid summary of the work." }
Send "assessment: skillId missing" POST $t1 "/api/skill-assessments" @{ claimedLevel = "BEGINNER"; evidenceSummary = "A valid summary of the work." }
Send "assessment: skillId malformed" POST $t1 "/api/skill-assessments" @{ skillId = "abc"; claimedLevel = "BEGINNER"; evidenceSummary = "A valid summary of the work." }

"--- 3: admin account management ---"
Send "employee changes a role (expect 403)" PATCH $t1 "/api/employees/$id1/role" @{ role = "ADMIN" }
Send "manager changes a role (expect 403)" PATCH $t3 "/api/employees/$id1/role" @{ role = "ADMIN" }
Send "admin promotes test.three (expect 200, role ADMIN)" PATCH $t2 "/api/employees/$id3/role" @{ role = "ADMIN" }
Send "admin sets test.three back (expect 200, role MANAGER)" PATCH $t2 "/api/employees/$id3/role" @{ role = "MANAGER" }
Send "admin demotes self (expect 409)" PATCH $t2 "/api/employees/$id2/role" @{ role = "MANAGER" }
Send "admin deactivates self (expect 409)" PATCH $t2 "/api/employees/$id2/active" @{ active = $false }
Send "unknown employee (expect 404)" PATCH $t2 "/api/employees/99999999-9999-9999-9999-999999999999/role" @{ role = "MANAGER" }
Send "malformed id (expect 400)" PATCH $t2 "/api/employees/not-a-uuid/role" @{ role = "MANAGER" }
Send "bad role value (expect 400)" PATCH $t2 "/api/employees/$id3/role" @{ role = "BOSS" }
Send "missing role (expect 400)" PATCH $t2 "/api/employees/$id3/role" @{}
Send "deactivate test.one (expect 200, active false)" PATCH $t2 "/api/employees/$id1/active" @{ active = $false }
Status "deactivated user, old token (expect 401)" GET $t1 "/api/employees/me" $null
Status "deactivated user logs in (expect 401)" POST $null "/api/auth/login" "login-ok.json"
Send "reactivate test.one (expect 200, active true)" PATCH $t2 "/api/employees/$id1/active" @{ active = $true }
Status "reactivated user, same old token (expect 200)" GET $t1 "/api/employees/me" $null
Status "reactivated user logs in (expect 200)" POST $null "/api/auth/login" "login-ok.json"

Remove-Item (Join-Path $PSScriptRoot "tmp-body.json") -ErrorAction SilentlyContinue
"--- done ---"