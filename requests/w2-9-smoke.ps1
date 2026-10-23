# W2-9 smoke test: the whole product story with the seeded demo accounts. Dummy data only. Prints no tokens.
# Run once against a fresh database (a second pass hits the already-verified rule).
$base = "http://localhost:8080"
$demo = "Demo-Pass-123"
$java   = "aaaaaaaa-0000-0000-0000-000000000001"
$docker = "aaaaaaaa-0000-0000-0000-000000000005"
$script:checks = 0
$script:fails = 0

function Call($method, $token, $path, $obj) {
    $out = Join-Path $PSScriptRoot "tmp-response.json"
    $a = @("-s", "-X", $method, "-o", $out, "-w", "%{http_code}")
    if ($token) { $a += @("-H", "Authorization: Bearer $token") }
    if ($null -ne $obj) {
        [IO.File]::WriteAllText((Join-Path $PSScriptRoot "tmp-body.json"), ($obj | ConvertTo-Json -Compress))
        $a += @("-H", "Content-Type: application/json", "-d", "@requests/tmp-body.json")
    }
    $status = curl.exe @a "$base$path"
    $text = ""
    if (Test-Path $out) { $text = [IO.File]::ReadAllText($out) }
    $json = $null
    if ($text) { try { $json = $text | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Status = [int]$status; Json = $json }
}
function Check($label, $actual, $expected) {
    $script:checks++
    if ("$actual" -eq "$expected") { "PASS  $label" }
    else { $script:fails++; "FAIL  $label (got '$actual', expected '$expected')" }
}
function Rows($r) { return @($r.Json | ForEach-Object { $_ }) }

"waiting for the app..."
$tries = 0
do { Start-Sleep -Seconds 1; $tries++; $up = curl.exe -s -o NUL -w "%{http_code}" "$base/api/skills" } while ($up -ne "401" -and $tries -lt 120)
if ($up -ne "401") { "STOP: the app did not come up"; return }
"app is up"

"--- demo logins ---"
$a = Call POST $null "/api/auth/login" @{ email = "ada@skillbridge.local";   password = $demo }
$m = Call POST $null "/api/auth/login" @{ email = "tunde@skillbridge.local"; password = $demo }
$c = Call POST $null "/api/auth/login" @{ email = "chidi@skillbridge.local"; password = $demo }
Check "Ada logs in" $a.Status 200
Check "Tunde logs in" $m.Status 200
Check "Chidi logs in" $c.Status 200
$tA = $a.Json.accessToken; $tM = $m.Json.accessToken; $tC = $c.Json.accessToken
if (-not $tA -or -not $tM -or -not $tC) { "STOP: a demo login failed. Was V5 applied?"; return }
Check "Ada is an EMPLOYEE" (Call GET $tA "/api/employees/me" $null).Json.role "EMPLOYEE"
Check "Tunde is a MANAGER" (Call GET $tM "/api/employees/me" $null).Json.role "MANAGER"
Check "Chidi is an ADMIN"  (Call GET $tC "/api/employees/me" $null).Json.role "ADMIN"
Check "wrong password is refused" (Call POST $null "/api/auth/login" @{ email = "ada@skillbridge.local"; password = "wrong-password-1" }).Status 401
Check "no token is refused" (Call GET $null "/api/skills" $null).Status 401
Check "unknown path uses the error format" (Call GET $tA "/api/nothing-here" $null).Json.error.code "NOT_FOUND"

"--- skills and permissions ---"
Check "six seeded skills or more" ((Rows (Call GET $tA "/api/skills" $null)).Count -ge 6) "True"
Check "employee cannot create a skill" (Call POST $tA "/api/skills" @{ name = "Nope"; category = "Test" }).Status 403
$stamp = Get-Date -Format "yyyyMMddHHmmss"
Check "admin creates a skill" (Call POST $tC "/api/skills" @{ name = "Smoke skill $stamp"; category = "Demo" }).Status 201

"--- claim, review ---"
$claim = Call POST $tA "/api/skill-assessments" @{ skillId = $docker; claimedLevel = "BEGINNER"; evidenceSummary = "Containerised the demo service with a Dockerfile." }
Check "Ada claims Docker" $claim.Status 201
$aid = $claim.Json.id
if (-not $aid) { "STOP: no assessment id (was this database already used?)"; return }
Check "Ada cannot verify" (Call PATCH $tA "/api/skill-assessments/$aid/verify" @{ reviewComment = "Looks fine" }).Status 403
$ver = Call PATCH $tM "/api/skill-assessments/$aid/verify" @{ reviewComment = "Checked the Dockerfile." }
Check "Tunde verifies" $ver.Status 200
Check "status is VERIFIED" $ver.Json.status "VERIFIED"

"--- opportunity, match ---"
$opp = Call POST $tM "/api/opportunities" @{ title = "Platform Engineer (smoke)"; description = "Internal move to the platform team."; closingDate = "2030-12-31" }
Check "Tunde creates a DRAFT" $opp.Status 201
$oid = $opp.Json.id
if (-not $oid) { "STOP: no opportunity id"; return }
Check "require Java INTERMEDIATE" (Call POST $tM "/api/opportunities/$oid/requirements" @{ skillId = $java; minimumLevel = "INTERMEDIATE" }).Status 201
Check "require Docker BEGINNER" (Call POST $tM "/api/opportunities/$oid/requirements" @{ skillId = $docker; minimumLevel = "BEGINNER" }).Status 201
Check "Ada cannot see a DRAFT" (Call GET $tA "/api/opportunities/$oid" $null).Status 403
Check "Tunde publishes" (Call PATCH $tM "/api/opportunities/$oid/publish" $null).Json.status "PUBLISHED"
$match = Call GET $tA "/api/opportunities/$oid/match" $null
Check "match: requirements met" $match.Json.requirementsMet 1
Check "match: total requirements" $match.Json.totalRequirements 2
Check "match: percentage" $match.Json.matchPercentage 50

"--- applications ---"
$app = Call POST $tA "/api/applications" @{ opportunityId = $oid }
Check "Ada applies" $app.Status 201
Check "application is SUBMITTED" $app.Json.status "SUBMITTED"
$pid = $app.Json.id
Check "Ada cannot apply twice" (Call POST $tA "/api/applications" @{ opportunityId = $oid }).Status 409
Check "Tunde cannot apply to his own opportunity" (Call POST $tM "/api/applications" @{ opportunityId = $oid }).Status 409
Check "Tunde sees one application" (Rows (Call GET $tM "/api/applications?opportunityId=$oid" $null)).Count 1
Check "Tunde shortlists" (Call PATCH $tM "/api/applications/$pid/shortlist" $null).Json.status "SHORTLISTED"
Check "Ada sees SHORTLISTED" (Rows (Call GET $tA "/api/applications" $null))[0].status "SHORTLISTED"
Check "Chidi (admin) sees all" ((Rows (Call GET $tC "/api/applications" $null)).Count -ge 1) "True"

"--- admin ---"
Check "Ada cannot change a role" (Call PATCH $tA "/api/employees/11111111-1111-1111-1111-111111111111/role" @{ role = "ADMIN" }).Status 403
Check "admin cannot deactivate himself" (Call PATCH $tC "/api/employees/33333333-3333-3333-3333-333333333333/active" @{ active = $false }).Status 409

Remove-Item (Join-Path $PSScriptRoot "tmp-body.json") -ErrorAction SilentlyContinue
Remove-Item (Join-Path $PSScriptRoot "tmp-response.json") -ErrorAction SilentlyContinue
""
if ($script:fails -eq 0) { "ALL $($script:checks) CHECKS PASSED" } else { "$($script:fails) of $($script:checks) CHECKS FAILED" }