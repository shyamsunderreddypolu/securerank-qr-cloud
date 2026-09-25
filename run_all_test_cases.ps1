$ErrorActionPreference = "Continue"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "         SECURERANK COMPREHENSIVE LOCALHOST MANUAL TEST SUITE         " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$apiBase = "$baseUrl/api"

$passCount = 0
$failCount = 0

function Assert-Test([string]$testName, [bool]$condition, [string]$details = "") {
    if ($condition) {
        $global:passCount++
        Write-Host "[PASS] $testName" -ForegroundColor Green
        if ($details) { Write-Host "       $details" -ForegroundColor Gray }
    } else {
        $global:failCount++
        Write-Host "[FAIL] $testName" -ForegroundColor Red
        if ($details) { Write-Host "       $details" -ForegroundColor Magenta }
    }
}

function Upload-Multipart([string]$url, [string]$token, [string]$filePath, [string]$label, [string]$keywords) {
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
    $fileName = [System.IO.Path]::GetFileName($filePath)
    $fileEnc = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)

    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"label`"$LF",
        $label,
        "--$boundary",
        "Content-Disposition: form-data; name=`"keywords`"$LF",
        $keywords,
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
        "Content-Type: text/plain$LF",
        $fileEnc,
        "--$boundary--$LF"
    ) -join $LF

    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "multipart/form-data; boundary=$boundary"
    }

    return Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $bodyLines
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 1: PUBLIC ASSETS & STATIC CONTENT ACCESSIBILITY" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

# Case 1: Root homepage loads
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing
    Assert-Test "Case 1: Homepage (index.html) loads publicly with 200 OK" ($r.StatusCode -eq 200) "Status: $($r.StatusCode)"
} catch {
    Assert-Test "Case 1: Homepage loads" $false $_.Exception.Message
}

# Case 2: app.js loads publicly
try {
    $r = Invoke-WebRequest -Uri "$baseUrl/app.js" -UseBasicParsing
    Assert-Test "Case 2: Frontend logic (app.js) loads publicly with 200 OK" ($r.StatusCode -eq 200 -and $r.Content.Length -gt 1000) "Length: $($r.Content.Length) bytes"
} catch {
    Assert-Test "Case 2: app.js loads" $false $_.Exception.Message
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 2: AUTHENTICATION, REGISTRATION & USER APPROVAL WORKFLOW" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

# Case 3: Bad credentials login rejection
try {
    $badLogin = @{ email = "owner@securerank.com"; password = "wrongpassword" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$apiBase/auth/login" -Method Post -Body $badLogin -ContentType "application/json"
    Assert-Test "Case 3: Login with invalid password rejected" $false "Expected 401 but got response"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 3: Login with invalid password rejected with 401 Unauthorized" ($statusCode -eq 401) "Status: $statusCode"
}

# Case 4: Registration with short password (< 6 chars) rejected
try {
    $shortPassReq = @{ name = "Short Pass"; email = "short@test.com"; password = "123"; role = "ROLE_CONSUMER" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$apiBase/auth/register" -Method Post -Body $shortPassReq -ContentType "application/json"
    Assert-Test "Case 4: Register with password < 6 chars rejected" $false "Expected 400 Bad Request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 4: Register with password < 6 chars rejected with 400 Bad Request" ($statusCode -eq 400) "Status: $statusCode"
}

# Case 5: Register new Data Owner (starts unapproved)
$uniqueId = [System.Environment]::TickCount
$newOwnerEmail = "owner_$uniqueId@securerank.com"
$newOwnerPass = "Pass1234!"
try {
    $regOwnerReq = @{ name = "Dr. Carter"; email = $newOwnerEmail; password = $newOwnerPass; role = "ROLE_OWNER"; mobile = "9876543210" } | ConvertTo-Json
    $regRes = Invoke-RestMethod -Uri "$apiBase/auth/register" -Method Post -Body $regOwnerReq -ContentType "application/json"
    Assert-Test "Case 5: Register new Data Owner succeeds (pending approval)" ($regRes.success -eq $true) "Message: $($regRes.message)"
} catch {
    Assert-Test "Case 5: Register new Data Owner" $false $_.Exception.Message
}

# Case 6: Reject duplicate registration with same email
try {
    $dupReq = @{ name = "Duplicate"; email = $newOwnerEmail; password = $newOwnerPass; role = "ROLE_OWNER" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$apiBase/auth/register" -Method Post -Body $dupReq -ContentType "application/json"
    Assert-Test "Case 6: Duplicate email registration rejected" $false "Expected 400 Bad Request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 6: Duplicate email registration rejected with 400 Bad Request" ($statusCode -eq 400) "Status: $statusCode"
}

# Case 7: Register new Data Consumer (starts unapproved)
$newConsumerEmail = "consumer_$uniqueId@securerank.com"
$newConsumerPass = "Pass1234!"
try {
    $regConsumerReq = @{ name = "David Researcher"; email = $newConsumerEmail; password = $newConsumerPass; role = "ROLE_CONSUMER"; mobile = "9123456780" } | ConvertTo-Json
    $regRes = Invoke-RestMethod -Uri "$apiBase/auth/register" -Method Post -Body $regConsumerReq -ContentType "application/json"
    Assert-Test "Case 7: Register new Data Consumer succeeds (pending approval)" ($regRes.success -eq $true) "Message: $($regRes.message)"
} catch {
    Assert-Test "Case 7: Register new Data Consumer" $false $_.Exception.Message
}

# Case 8: Unapproved account login rejected with 403 Forbidden
try {
    $unapprLogin = @{ email = $newOwnerEmail; password = $newOwnerPass } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$apiBase/auth/login" -Method Post -Body $unapprLogin -ContentType "application/json"
    Assert-Test "Case 8: Unapproved user login blocked" $false "Expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 8: Unapproved user login rejected with 403 Forbidden (pending approval)" ($statusCode -eq 403) "Status: $statusCode"
}

# Case 9: Admin login to approve accounts
$adminToken = ""
try {
    $adminLogin = @{ email = "admin@securerank.com"; password = "admin123" } | ConvertTo-Json
    $adminRes = Invoke-RestMethod -Uri "$apiBase/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
    $adminToken = $adminRes.token
    Assert-Test "Case 9: System Admin login" ($adminRes.role -eq "ROLE_ADMIN" -and $adminToken.Length -gt 20) "Role: $($adminRes.role)"
} catch {
    Assert-Test "Case 9: System Admin login" $false $_.Exception.Message
}

# Case 10: Admin approves newly registered users
$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
try {
    $pendingUsers = Invoke-RestMethod -Uri "$apiBase/admin/pending-users" -Method Get -Headers $adminHeaders
    $targetOwner = $pendingUsers | Where-Object { $_.email -eq $newOwnerEmail } | Select-Object -First 1
    $targetConsumer = $pendingUsers | Where-Object { $_.email -eq $newConsumerEmail } | Select-Object -First 1
    
    $apprOwner = Invoke-RestMethod -Uri "$apiBase/admin/approve-user/$($targetOwner.id)" -Method Post -Headers $adminHeaders
    $apprConsumer = Invoke-RestMethod -Uri "$apiBase/admin/approve-user/$($targetConsumer.id)" -Method Post -Headers $adminHeaders
    
    Assert-Test "Case 10: Admin approves newly registered Data Owner and Consumer" ($apprOwner.success -and $apprConsumer.success) "Owner ID: $($targetOwner.id), Consumer ID: $($targetConsumer.id)"
} catch {
    Assert-Test "Case 10: Admin approves users" $false $_.Exception.Message
}

# Case 11: Newly approved Data Owner logs in successfully
$ownerToken = ""
try {
    $ownerLogin = @{ email = $newOwnerEmail; password = $newOwnerPass } | ConvertTo-Json
    $ownerRes = Invoke-RestMethod -Uri "$apiBase/auth/login" -Method Post -Body $ownerLogin -ContentType "application/json"
    $ownerToken = $ownerRes.token
    Assert-Test "Case 11: Newly approved Data Owner logs in successfully" ($ownerRes.role -eq "ROLE_OWNER" -and $ownerToken.Length -gt 20) "Role: $($ownerRes.role)"
} catch {
    Assert-Test "Case 11: Owner login after approval" $false $_.Exception.Message
}

# Case 12: Newly approved Data Consumer logs in successfully
$consumerToken = ""
try {
    $consumerLogin = @{ email = $newConsumerEmail; password = $newConsumerPass } | ConvertTo-Json
    $consumerRes = Invoke-RestMethod -Uri "$apiBase/auth/login" -Method Post -Body $consumerLogin -ContentType "application/json"
    $consumerToken = $consumerRes.token
    Assert-Test "Case 12: Newly approved Data Consumer logs in successfully" ($consumerRes.role -eq "ROLE_CONSUMER" -and $consumerToken.Length -gt 20) "Role: $($consumerRes.role)"
} catch {
    Assert-Test "Case 12: Consumer login after approval" $false $_.Exception.Message
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 3: DATA OWNER UPLOAD, AES-256 ENCRYPTION & METADATA" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

# Case 13: Upload Document 1 (Cardiology)
try {
    $upRes = Upload-Multipart "$apiBase/files/upload" $ownerToken "c:\Users\polus\eclipse-workspace\SecureRank\doc1_cardiology.txt" "Cardiology & Cardiac Report" "cardiology, cardiac, ecg, heart, blood pressure, risk"
    Assert-Test "Case 13: Upload & encrypt Document 1 (Cardiology)" ($upRes.success -eq $true) "Response: $($upRes.message)"
} catch {
    Assert-Test "Case 13: Upload Document 1" $false $_.Exception.Message
}

# Case 14: Upload Document 2 (Diabetes)
try {
    $upRes = Upload-Multipart "$apiBase/files/upload" $ownerToken "c:\Users\polus\eclipse-workspace\SecureRank\doc2_diabetes.txt" "Diabetes Care & Treatment" "diabetes, insulin, glucose, sugar, dietary, glycemic"
    Assert-Test "Case 14: Upload & encrypt Document 2 (Diabetes)" ($upRes.success -eq $true) "Response: $($upRes.message)"
} catch {
    Assert-Test "Case 14: Upload Document 2" $false $_.Exception.Message
}

# Case 15: Upload Document 3 (General Health)
try {
    $upRes = Upload-Multipart "$apiBase/files/upload" $ownerToken "c:\Users\polus\eclipse-workspace\SecureRank\doc3_general_health.txt" "Annual General Health Checkup" "general, health, wellness, cholesterol, fitness, vitals, blood test"
    Assert-Test "Case 15: Upload & encrypt Document 3 (General Health)" ($upRes.success -eq $true) "Response: $($upRes.message)"
} catch {
    Assert-Test "Case 15: Upload Document 3" $false $_.Exception.Message
}

# Case 16: Data Owner queries their uploaded files
$ownerFiles = @()
$doc1Id = 0
$doc2Id = 0
$doc3Id = 0
try {
    $ownerFiles = Invoke-RestMethod -Uri "$apiBase/files/my-files" -Method Get -Headers @{ "Authorization" = "Bearer $ownerToken" }
    $doc1 = $ownerFiles | Where-Object { $_.filename -eq "doc1_cardiology.txt" } | Select-Object -First 1
    $doc2 = $ownerFiles | Where-Object { $_.filename -eq "doc2_diabetes.txt" } | Select-Object -First 1
    $doc3 = $ownerFiles | Where-Object { $_.filename -eq "doc3_general_health.txt" } | Select-Object -First 1
    $doc1Id = $doc1.id
    $doc2Id = $doc2.id
    $doc3Id = $doc3.id
    Assert-Test "Case 16: Data Owner retrieves uploaded files list" ($ownerFiles.Count -ge 3 -and $doc1Id -gt 0 -and $doc2Id -gt 0) "Found $($ownerFiles.Count) files. Doc1 ID: $doc1Id, Doc2 ID: $doc2Id, Doc3 ID: $doc3Id"
} catch {
    Assert-Test "Case 16: Owner retrieves files list" $false $_.Exception.Message
}

# Case 17: Unauthenticated access to /api/files/my-files rejected
try {
    $r = Invoke-RestMethod -Uri "$apiBase/files/my-files" -Method Get
    Assert-Test "Case 17: Unauthenticated access to /my-files rejected" $false "Expected 401 Unauthorized"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 17: Unauthenticated access to /my-files rejected with 401 Unauthorized" ($statusCode -eq 401) "Status: $statusCode"
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 4: MULTI-KEYWORD SEARCH ENGINE & TF-IDF RANKING" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

$consumerHeaders = @{ "Authorization" = "Bearer $consumerToken" }

# Case 18: Single-keyword targeted search ("insulin")
try {
    $res18 = Invoke-RestMethod -Uri "$apiBase/files/search?query=insulin" -Method Get -Headers $consumerHeaders
    $topDoc = $res18 | Select-Object -First 1
    Assert-Test "Case 18: Targeted single-keyword search for 'insulin'" ($res18.Count -ge 1 -and $topDoc.filename -eq "doc2_diabetes.txt") "Rank #1: $($topDoc.filename), Score: $($topDoc.score)"
} catch {
    Assert-Test "Case 18: Single-keyword search" $false $_.Exception.Message
}

# Case 19: Multi-keyword ranked search ("cardiology ecg cardiac")
try {
    $res19 = Invoke-RestMethod -Uri "$apiBase/files/search?query=cardiology+ecg+cardiac" -Method Get -Headers $consumerHeaders
    $topDoc = $res19 | Select-Object -First 1
    Assert-Test "Case 19: Multi-keyword ranked search ('cardiology ecg cardiac')" ($res19.Count -ge 1 -and $topDoc.filename -eq "doc1_cardiology.txt" -and $topDoc.rank -eq 1) "Rank #1: $($topDoc.filename), Score: $($topDoc.score)"
} catch {
    Assert-Test "Case 19: Multi-keyword ranked search" $false $_.Exception.Message
}

# Case 20: Cross-document search ("blood")
try {
    $res20 = Invoke-RestMethod -Uri "$apiBase/files/search?query=blood" -Method Get -Headers $consumerHeaders
    Assert-Test "Case 20: Cross-document multi-hit search ('blood')" ($res20.Count -ge 2) "Returned $($res20.Count) matching ranked documents"
} catch {
    Assert-Test "Case 20: Cross-document search" $false $_.Exception.Message
}

# Case 21: Non-matching keywords search ("quantum astronomy physics")
try {
    $res21 = Invoke-RestMethod -Uri "$apiBase/files/search?query=quantum+astronomy+physics" -Method Get -Headers $consumerHeaders
    Assert-Test "Case 21: Non-matching query ('quantum astronomy physics') returns 0 results cleanly" ($res21.Count -eq 0) "Results: $($res21.Count)"
} catch {
    Assert-Test "Case 21: Non-matching search" $false $_.Exception.Message
}

# Case 22: Empty / whitespace search query
try {
    $res22 = Invoke-RestMethod -Uri "$apiBase/files/search?query=" -Method Get -Headers $consumerHeaders
    Assert-Test "Case 22: Empty search query returns empty list without error" ($res22.Count -eq 0) "Results: $($res22.Count)"
} catch {
    Assert-Test "Case 22: Empty search query" $false $_.Exception.Message
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 5: ACCESS CONTROL & SECRET KEY DISTRIBUTION" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

# Case 23: Consumer requests secret key for Document 1
try {
    $reqRes = Invoke-RestMethod -Uri "$apiBase/files/request-key/$doc1Id" -Method Post -Headers $consumerHeaders
    Assert-Test "Case 23: Data Consumer requests decryption key for Document 1" ($reqRes.success -eq $true) "Message: $($reqRes.message)"
} catch {
    Assert-Test "Case 23: Consumer requests key" $false $_.Exception.Message
}

# Case 24: Consumer submits duplicate request while pending
try {
    $dupReqRes = Invoke-RestMethod -Uri "$apiBase/files/request-key/$doc1Id" -Method Post -Headers $consumerHeaders
    Assert-Test "Case 24: Duplicate key request handled cleanly (pending notification)" ($dupReqRes.success -eq $false) "Message: $($dupReqRes.message)"
} catch {
    Assert-Test "Case 24: Duplicate key request" $false $_.Exception.Message
}

# Case 25: Admin inspects system metrics
try {
    $stats = Invoke-RestMethod -Uri "$apiBase/admin/stats" -Method Get -Headers $adminHeaders
    Assert-Test "Case 25: Admin dashboard metrics retrieved" ($stats.totalUsers -ge 5 -and $stats.totalFiles -ge 3 -and $stats.pendingKeyRequests -ge 1) "Total Users: $($stats.totalUsers), Files: $($stats.totalFiles), Pending Keys: $($stats.pendingKeyRequests)"
} catch {
    Assert-Test "Case 25: Admin inspects stats" $false $_.Exception.Message
}

# Case 26: Admin views pending key requests queue
$pendingReqId = 0
try {
    $pendingKeys = Invoke-RestMethod -Uri "$apiBase/admin/pending-keys" -Method Get -Headers $adminHeaders
    $targetReq = $pendingKeys | Where-Object { $_.fileId -eq $doc1Id -and $_.consumerEmail -eq $newConsumerEmail } | Select-Object -First 1
    $pendingReqId = $targetReq.requestId
    Assert-Test "Case 26: Admin locates pending key request in approval queue" ($pendingReqId -gt 0) "Request ID: $pendingReqId for File: $($targetReq.filename)"
} catch {
    Assert-Test "Case 26: Admin views pending keys" $false $_.Exception.Message
}

# Case 27: Admin approves secret key request
try {
    $apprRes = Invoke-RestMethod -Uri "$apiBase/admin/approve-key/$pendingReqId" -Method Post -Headers $adminHeaders
    Assert-Test "Case 27: Admin approves secret key request" ($apprRes.success -eq $true) "Message: $($apprRes.message)"
} catch {
    Assert-Test "Case 27: Admin approves key request" $false $_.Exception.Message
}

# Case 28: Consumer views approved key in /my-requests
$issuedMasterKey = ""
try {
    $myReqs = Invoke-RestMethod -Uri "$apiBase/files/my-requests" -Method Get -Headers $consumerHeaders
    $approvedDoc1Req = $myReqs | Where-Object { $_.fileId -eq $doc1Id } | Select-Object -First 1
    $issuedMasterKey = $approvedDoc1Req.masterKey
    Assert-Test "Case 28: Consumer receives APPROVED status and Master Decryption Key" ($approvedDoc1Req.status -eq "APPROVED" -and $issuedMasterKey.Length -gt 10) "Status: $($approvedDoc1Req.status), Key: $issuedMasterKey"
} catch {
    Assert-Test "Case 28: Consumer views approved key" $false $_.Exception.Message
}

# ----------------------------------------------------------------------------------
Write-Host "`n>>> CATEGORY 6: DECRYPTION, FILE DOWNLOAD & SECURITY RESTRICTIONS" -ForegroundColor Yellow
# ----------------------------------------------------------------------------------

# Case 29: Consumer downloads approved Document 1 (Verifies AES decryption against original file)
try {
    $decryptedContent = Invoke-RestMethod -Uri "$apiBase/files/download/$doc1Id" -Method Get -Headers $consumerHeaders
    $origDoc1Content = [System.IO.File]::ReadAllText("c:\Users\polus\eclipse-workspace\SecureRank\doc1_cardiology.txt")
    $matches = ($origDoc1Content.Trim() -eq $decryptedContent.Trim())
    Assert-Test "Case 29: Consumer downloads & decrypts Document 1 with byte-for-byte fidelity" $matches "Content Length: $($decryptedContent.Length) chars, Matches Original: $matches"
} catch {
    Assert-Test "Case 29: Consumer downloads Document 1" $false $_.Exception.Message
}

# Case 30: Data Owner downloads their own Document 2 without needing approval
try {
    $ownerDoc2Content = Invoke-RestMethod -Uri "$apiBase/files/download/$doc2Id" -Method Get -Headers @{ "Authorization" = "Bearer $ownerToken" }
    $origDoc2Content = [System.IO.File]::ReadAllText("c:\Users\polus\eclipse-workspace\SecureRank\doc2_diabetes.txt")
    $ownerMatches = ($origDoc2Content.Trim() -eq $ownerDoc2Content.Trim())
    Assert-Test "Case 30: Data Owner can directly download & decrypt their own file" $ownerMatches "Content Length: $($ownerDoc2Content.Length) chars, Matches: $ownerMatches"
} catch {
    Assert-Test "Case 30: Owner downloads own file" $false $_.Exception.Message
}

# Case 31: Unauthorized download: Consumer attempts to download Document 2 without approved key
try {
    $unauthDownload = Invoke-RestMethod -Uri "$apiBase/files/download/$doc2Id" -Method Get -Headers $consumerHeaders
    Assert-Test "Case 31: Unauthorized file download without approved key rejected" $false "Expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 31: Unauthorized file download rejected with 403 Forbidden (Access Denied)" ($statusCode -eq 403) "Status: $statusCode"
}

# Case 32: RBAC check: Consumer attempts to access Admin endpoint /api/admin/stats
try {
    $unauthStats = Invoke-RestMethod -Uri "$apiBase/admin/stats" -Method Get -Headers $consumerHeaders
    Assert-Test "Case 32: Consumer accessing Admin endpoint rejected" $false "Expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 32: Consumer accessing Admin endpoint rejected with 403 Forbidden" ($statusCode -eq 403) "Status: $statusCode"
}

# Case 33: RBAC check: Owner attempts to access Admin endpoint /api/admin/stats
try {
    $ownerStats = Invoke-RestMethod -Uri "$apiBase/admin/stats" -Method Get -Headers @{ "Authorization" = "Bearer $ownerToken" }
    Assert-Test "Case 33: Owner accessing Admin endpoint rejected" $false "Expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "Case 33: Owner accessing Admin endpoint rejected with 403 Forbidden" ($statusCode -eq 403) "Status: $statusCode"
}

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host "                       FINAL TEST RESULTS SUMMARY                     " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "TOTAL TESTS RUN: $($passCount + $failCount)" -ForegroundColor White
Write-Host "PASSED:          $passCount" -ForegroundColor Green
Write-Host "FAILED:          $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "SUCCESS RATE:    $([math]::Round(($passCount / ($passCount + $failCount)) * 100, 2))%" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "======================================================================" -ForegroundColor Cyan
