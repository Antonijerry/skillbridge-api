# W2-7 verification: match percentage and applications. Dummy accounts only. Prints no tokens.
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
function Json($method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a "$base$path" | ConvertFrom-Json
}
function Sql($query) {
    docker compose exec db psql -U skillbridge -d skillbridge -c $query
}
function Body($obj) {
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot "tmp-body.json"), ($obj | ConvertTo-Json -Compress))
}
function Send($label, $method, $token, $path, $obj) {
    Body $obj
    Show $label $method $token $path "tmp-body.json"
}
function Count($label, $token, $path, $expected) {
    $r = Json GET $token $path $null
    $n = @($r | ForEach-Object { $_ }).Count
    "${label}: $n rows (expect $expected)"
}
function NewOpp($token, $title, $reqs) {
    Body @{ title = $title; description = "Created by the W2-7 checks to exercise matching."; closingDate = "2030-12-31" }
    $id = (Json POST $token "/api/opportunities" "tmp-body.json").id
    foreach ($r in $reqs) {
        Body @{ skillId = $r[0]; minimumLevel = $r[1] }
        $null = Json POST $token "/api/opportunities/$id/requirements" "tmp-body.json"
    }
    return $id
}
function Publish($token, $id) { $null = Json PATCH $token "/api/opportunities/$id/publish" $null }

$java   = "aaaaaaaa-0000-0000-0000-000000000001"
$sqlSk  = "aaaaaaaa-0000-0000-0000-000000000002"
$docker = "aaaaaaaa-0000-0000-0000-000000000005"
$nobody = "99999999-9999-9999-9999-999999999999"

"waiting for the app..."
do { Start-Sleep -Seconds 1; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skill-assessments" } while ($up -ne "401")
"app is up"

$setup = @"
DELETE FROM applications WHERE employee_id IN (SELECT id FROM employees WHERE email LIKE 'test.%') OR opportunity_id IN (SELECT id FROM opportunities WHERE created_by IN (SELECT id FROM employees WHERE email LIKE 'test.%'));
DELETE FROM opportunities WHERE created_by IN (SELECT id FROM employees WHERE email LIKE 'test.%');
DELETE FROM skill_assessments WHERE employee_id IN (SELECT id FROM employees WHERE email LIKE 'test.%');
UPDATE skills SET active=true;
UPDATE employees SET active=true WHERE email LIKE 'test.%';
UPDATE employees SET role='EMPLOYEE' WHERE email='test.one@example.com';
UPDATE employees SET role='MANAGER' WHERE email IN ('test.two@example.com','test.three@example.com');
INSERT INTO skill_assessments (employee_id, skill_id, claimed_level, evidence_summary, status, reviewer_id, reviewed_at, review_comment)
SELECT e.id, v.skill::uuid, v.level, 'Seeded for the W2-7 checks.', v.status,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE '22222222-2222-2222-2222-222222222222'::uuid END,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE now() END,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE 'seeded' END
FROM employees e,
     (VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'ADVANCED',     'VERIFIED'),
             ('aaaaaaaa-0000-0000-0000-000000000002', 'BEGINNER',     'VERIFIED'),
             ('aaaaaaaa-0000-0000-0000-000000000005', 'INTERMEDIATE', 'VERIFIED'),
             ('aaaaaaaa-0000-0000-0000-000000000002', 'EXPERT',       'PENDING'),
             ('aaaaaaaa-0000-0000-0000-000000000005', 'EXPERT',       'REJECTED')) AS v(skill, level, status)
WHERE e.email = 'test.one@example.com';
"@
Sql ($setup -replace "\r?\n", " ")
"(the last statement must say INSERT 0 5)"

$t1 = (Json POST $null "/api/auth/login" "login-ok.json").accessToken
$t2 = (Json POST $null "/api/auth/login" "login-two.json").accessToken
$t3 = (Json POST $null "/api/auth/login" "login-three.json").accessToken
if (-not $t1 -or -not $t2 -or -not $t3) { "STOP: login failed (run w2-6-checks.ps1 once first)"; return }
$id1 = (Json GET $t1 "/api/employees/me" $null).id
$id2 = (Json GET $t2 "/api/employees/me" $null).id
if (-not $id1 -or -not $id2) { "STOP: could not read the accounts"; return }
"ready (one = EMPLOYEE, two = MANAGER and creator, three = MANAGER)"

$oppA = NewOpp $t2 "W2-7 match target" @( @($java, "INTERMEDIATE"), @($sqlSk, "BEGINNER"), @($docker, "ADVANCED") )
$oppB = NewOpp $t2 "W2-7 draft" @( ,@($java, "BEGINNER") )
if (-not $oppA -or -not $oppB) { "STOP: could not create the opportunities"; return }
"created A (3 requirements, will be published) and B (stays DRAFT)"

"--- 1: match ---"
Show "employee asks about a DRAFT (expect 403)" GET $t1 "/api/opportunities/$oppA/match" $null
Show "manager, own match on a DRAFT (expect 200, 0 of 3, 0 percent)" GET $t2 "/api/opportunities/$oppA/match" $null
Publish $t2 $oppA
$m = Json GET $t1 "/api/opportunities/$oppA/match" $null
$m | ConvertTo-Json -Depth 5
"met=$($m.requirementsMet) total=$($m.totalRequirements) percent=$($m.matchPercentage)   (expect met=2 total=3 percent=66)"
Show "employee asks for someone else (expect 403)" GET $t1 "/api/opportunities/$oppA/match?employeeId=$id2" $null
Show "manager asks for test.one (expect 200, 66)" GET $t2 "/api/opportunities/$oppA/match?employeeId=$id1" $null
Show "unknown employee (expect 404)" GET $t2 "/api/opportunities/$oppA/match?employeeId=$nobody" $null
Show "malformed employeeId (expect 400)" GET $t2 "/api/opportunities/$oppA/match?employeeId=abc" $null
Show "unknown opportunity (expect 404)" GET $t1 "/api/opportunities/$nobody/match" $null
Show "no token (expect 401)" GET $null "/api/opportunities/$oppA/match" $null

"--- 2: applying ---"
Send "missing opportunityId (expect 400)" POST $t1 "/api/applications" @{}
Send "unknown opportunity (expect 404)" POST $t1 "/api/applications" @{ opportunityId = $nobody }
Send "DRAFT opportunity (expect 409)" POST $t1 "/api/applications" @{ opportunityId = $oppB }
Send "creator applies to own opportunity (expect 409)" POST $t2 "/api/applications" @{ opportunityId = $oppA }
Send "employee applies to A (expect 201, SUBMITTED)" POST $t1 "/api/applications" @{ opportunityId = $oppA }
Send "apply again (expect 409 ALREADY_APPLIED)" POST $t1 "/api/applications" @{ opportunityId = $oppA }
Send "other manager applies to A (expect 201)" POST $t3 "/api/applications" @{ opportunityId = $oppA }
Show "no token (expect 401)" POST $null "/api/applications" "tmp-body.json"

$oppD = NewOpp $t2 "W2-7 expired" @( ,@($java, "BEGINNER") )
Publish $t2 $oppD
Sql "UPDATE opportunities SET closing_date='2020-01-01' WHERE id='$oppD';"
$oppE = NewOpp $t2 "W2-7 closed" @( ,@($java, "BEGINNER") )
Publish $t2 $oppE
$null = Json PATCH $t2 "/api/opportunities/$oppE/close" $null
Send "closing date has passed (expect 409)" POST $t1 "/api/applications" @{ opportunityId = $oppD }
Send "CLOSED opportunity (expect 409)" POST $t1 "/api/applications" @{ opportunityId = $oppE }

$app1 = @(Json GET $t1 "/api/applications" $null)[0].id
$app2 = @(Json GET $t3 "/api/applications" $null | Where-Object { $_.opportunityId -eq $oppA })[0].id
if (-not $app1 -or -not $app2) { "STOP: could not find the two applications"; return }

"--- 3: listing ---"
Count "employee (own only)" $t1 "/api/applications" 1
Count "creator manager (applications to A)" $t2 "/api/applications" 2
Count "other manager (only own)" $t3 "/api/applications" 1
Count "creator, status=SUBMITTED" $t2 "/api/applications?status=SUBMITTED" 2
Count "creator, status=SHORTLISTED" $t2 "/api/applications?status=SHORTLISTED" 0
Count "creator, opportunityId=A" $t2 "/api/applications?opportunityId=$oppA" 2
Show "bad status filter (expect 400)" GET $t2 "/api/applications?status=bogus" $null
Show "no token (expect 401)" GET $null "/api/applications" $null

"--- 4: decisions ---"
Show "employee shortlists (expect 403)" PATCH $t1 "/api/applications/$app1/shortlist" $null
Show "manager who is not the creator shortlists (expect 403)" PATCH $t3 "/api/applications/$app1/shortlist" $null
Show "unknown application (expect 404)" PATCH $t2 "/api/applications/$nobody/shortlist" $null
Show "malformed id (expect 400)" PATCH $t2 "/api/applications/not-a-uuid/shortlist" $null
Show "creator shortlists (expect 200 SHORTLISTED)" PATCH $t2 "/api/applications/$app1/shortlist" $null
Show "shortlist again (expect 409)" PATCH $t2 "/api/applications/$app1/shortlist" $null
Show "withdraw someone else's application (expect 403)" PATCH $t1 "/api/applications/$app2/withdraw" $null
Show "applicant withdraws own (expect 200 WITHDRAWN)" PATCH $t1 "/api/applications/$app1/withdraw" $null
Show "withdraw again (expect 409)" PATCH $t1 "/api/applications/$app1/withdraw" $null
Show "reject a withdrawn one (expect 409)" PATCH $t2 "/api/applications/$app1/reject" $null
Show "creator rejects app 2 (expect 200 REJECTED)" PATCH $t2 "/api/applications/$app2/reject" $null
Show "applicant withdraws a rejected one (expect 409)" PATCH $t3 "/api/applications/$app2/withdraw" $null
Send "re-apply after withdrawing (expect 409 ALREADY_APPLIED)" POST $t1 "/api/applications" @{ opportunityId = $oppA }
Sql "UPDATE employees SET role='ADMIN' WHERE email='test.three@example.com';"
Count "same token, now ADMIN, sees all" $t3 "/api/applications" 2

Remove-Item (Join-Path $PSScriptRoot "tmp-body.json") -ErrorAction SilentlyContinue
"--- done ---"