# Recount of the W2-7 listing rules against the data the main script left behind. Prints no tokens.
$base = "http://localhost:8080"

function BuildArgs($method, $token, $file) {
    $a = @("-s", "-X", $method)
    if ($token) { $a += @("-H", "Authorization: Bearer $token") }
    if ($file)  { $a += @("-H", "Content-Type: application/json", "-d", "@requests/$file") }
    return $a
}
function Json($method, $token, $path, $file) {
    $a = BuildArgs $method $token $file
    curl.exe @a "$base$path" | ConvertFrom-Json
}
function Sql($query) {
    docker compose exec db psql -U skillbridge -d skillbridge -c $query
}
function Count($label, $token, $path, $expected) {
    $r = Json GET $token $path $null
    $n = @($r | ForEach-Object { $_ }).Count
    "${label}: $n rows (expect $expected)"
}

$t1 = (Json POST $null "/api/auth/login" "login-ok.json").accessToken
$t2 = (Json POST $null "/api/auth/login" "login-two.json").accessToken
$t3 = (Json POST $null "/api/auth/login" "login-three.json").accessToken
if (-not $t1 -or -not $t2 -or -not $t3) { "STOP: login failed"; return }

$oppA = (@(Json GET $t2 "/api/applications" $null | ForEach-Object { $_ }))[0].opportunityId
if (-not $oppA) { "STOP: no applications found, run w2-7-checks.ps1 first"; return }

"state now: test.one = WITHDRAWN, test.three = REJECTED, both on opportunity A (created by test.two)"
Sql "UPDATE employees SET role='MANAGER' WHERE email='test.three@example.com';"
Count "employee (own only)" $t1 "/api/applications" 1
Count "creator manager (applications to A)" $t2 "/api/applications" 2
Count "other manager, now MANAGER again (only own)" $t3 "/api/applications" 1
Count "creator, status=WITHDRAWN" $t2 "/api/applications?status=WITHDRAWN" 1
Count "creator, status=REJECTED" $t2 "/api/applications?status=REJECTED" 1
Count "creator, status=SUBMITTED" $t2 "/api/applications?status=SUBMITTED" 0
Count "creator, opportunityId=A" $t2 "/api/applications?opportunityId=$oppA" 2
Count "employee, opportunityId=A" $t1 "/api/applications?opportunityId=$oppA" 1
Sql "UPDATE employees SET role='ADMIN' WHERE email='test.three@example.com';"
Count "same token, now ADMIN (all)" $t3 "/api/applications" 2
Sql "UPDATE employees SET role='MANAGER' WHERE email='test.three@example.com';"
"--- done ---"