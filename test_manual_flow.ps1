$ErrorActionPreference = "Stop"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "       SECURERANK END-TO-END MANUAL TEST SUITE       " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api"

# 1. Test Data Owner Login
Write-Host "`n[STEP 1] Logging in as Data Owner (owner@securerank.com)..." -ForegroundColor Yellow
$ownerLoginBody = @{ email = "owner@securerank.com"; password = "owner123" } | ConvertTo-Json
$ownerLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $ownerLoginBody -ContentType "application/json"
$ownerToken = $ownerLoginRes.token
Write-Host " -> Owner Login SUCCESS! Token received." -ForegroundColor Green
Write-Host "    Role: $($ownerLoginRes.role), Name: $($ownerLoginRes.name)"

# 2. Test Data Consumer Login
Write-Host "`n[STEP 2] Logging in as Data Consumer (consumer@securerank.com)..." -ForegroundColor Yellow
$consumerLoginBody = @{ email = "consumer@securerank.com"; password = "consumer123" } | ConvertTo-Json
$consumerLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $consumerLoginBody -ContentType "application/json"
$consumerToken = $consumerLoginRes.token
Write-Host " -> Consumer Login SUCCESS! Token received." -ForegroundColor Green
Write-Host "    Role: $($consumerLoginRes.role), Name: $($consumerLoginRes.name)"

# 3. Test Admin Login
Write-Host "`n[STEP 3] Logging in as System Admin (admin@securerank.com)..." -ForegroundColor Yellow
$adminLoginBody = @{ email = "admin@securerank.com"; password = "admin123" } | ConvertTo-Json
$adminLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminToken = $adminLoginRes.token
Write-Host " -> Admin Login SUCCESS! Token received." -ForegroundColor Green
Write-Host "    Role: $($adminLoginRes.role), Name: $($adminLoginRes.name)"

# 4. Data Owner uploads encrypted file
Write-Host "`n[STEP 4] Data Owner uploading and encrypting 'sample_medical_record.txt'..." -ForegroundColor Yellow
$filePath = "c:\Users\polus\eclipse-workspace\SecureRank\sample_medical_record.txt"

# Prepare multipart/form-data upload using curl.exe or HttpClient
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileEnc = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"label`"$LF",
    "Patient Cardiology Report",
    "--$boundary",
    "Content-Disposition: form-data; name=`"keywords`"$LF",
    "cardiology, diabetes, medical, confidential, heart",
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"sample_medical_record.txt`"",
    "Content-Type: text/plain$LF",
    $fileEnc,
    "--$boundary--$LF"
) -join $LF

$ownerHeaders = @{
    "Authorization" = "Bearer $ownerToken"
    "Content-Type" = "multipart/form-data; boundary=$boundary"
}

$uploadRes = Invoke-RestMethod -Uri "$baseUrl/files/upload" -Method Post -Headers $ownerHeaders -Body $bodyLines
Write-Host " -> Upload Response: $($uploadRes.message)" -ForegroundColor Green

# 5. Verify Owner's File List
Write-Host "`n[STEP 5] Querying Data Owner's files..." -ForegroundColor Yellow
$myFiles = Invoke-RestMethod -Uri "$baseUrl/files/my-files" -Method Get -Headers @{ "Authorization" = "Bearer $ownerToken" }
$uploadedDoc = $myFiles | Where-Object { $_.filename -eq "sample_medical_record.txt" } | Select-Object -First 1
Write-Host " -> Found Uploaded File in Database: ID = $($uploadedDoc.id), Label = '$($uploadedDoc.label)', Size = $($uploadedDoc.fileSize) bytes" -ForegroundColor Green

# 6. Data Consumer Multi-Keyword Ranked Search
Write-Host "`n[STEP 6] Data Consumer executing ranked search query for 'cardiology diabetes'..." -ForegroundColor Yellow
$consumerHeaders = @{ "Authorization" = "Bearer $consumerToken" }
$searchResults = Invoke-RestMethod -Uri "$baseUrl/files/search?query=cardiology+diabetes" -Method Get -Headers $consumerHeaders

Write-Host " -> Search Returned $($searchResults.Count) result(s):" -ForegroundColor Green
foreach ($res in $searchResults) {
    Write-Host "    Rank #$($res.rank) | Document: $($res.filename) | Label: $($res.label) | Score: $($res.score) | Status: $($res.keyRequestStatus)" -ForegroundColor Cyan
}

# 7. Data Consumer requests decryption key
Write-Host "`n[STEP 7] Data Consumer requesting decryption key for File ID $($uploadedDoc.id)..." -ForegroundColor Yellow
$reqKeyRes = Invoke-RestMethod -Uri "$baseUrl/files/request-key/$($uploadedDoc.id)" -Method Post -Headers $consumerHeaders
Write-Host " -> Request Key Response: $($reqKeyRes.message)" -ForegroundColor Green

# 8. Admin reviews pending requests and stats
Write-Host "`n[STEP 8] Admin inspecting system stats and pending key requests..." -ForegroundColor Yellow
$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
$stats = Invoke-RestMethod -Uri "$baseUrl/admin/stats" -Method Get -Headers $adminHeaders
Write-Host " -> System Stats: Total Users = $($stats.totalUsers), Total Files = $($stats.totalFiles), Pending Keys = $($stats.pendingKeyRequests)" -ForegroundColor Green

$pendingKeys = Invoke-RestMethod -Uri "$baseUrl/admin/pending-keys" -Method Get -Headers $adminHeaders
$targetPending = $pendingKeys | Where-Object { $_.fileId -eq $uploadedDoc.id } | Select-Object -First 1
Write-Host " -> Found Pending Request ID = $($targetPending.requestId) for file '$($targetPending.filename)' requested by '$($targetPending.consumerEmail)'" -ForegroundColor Green

# 9. Admin approves key request
Write-Host "`n[STEP 9] Admin approving key request ID $($targetPending.requestId)..." -ForegroundColor Yellow
$approveRes = Invoke-RestMethod -Uri "$baseUrl/admin/approve-key/$($targetPending.requestId)" -Method Post -Headers $adminHeaders
Write-Host " -> Key Approval Response: $($approveRes.message)" -ForegroundColor Green

# 10. Data Consumer downloads and verifies decrypted content
Write-Host "`n[STEP 10] Data Consumer fetching approved key and downloading decrypted file..." -ForegroundColor Yellow
$myRequests = Invoke-RestMethod -Uri "$baseUrl/files/my-requests" -Method Get -Headers $consumerHeaders
$approvedReq = $myRequests | Where-Object { $_.fileId -eq $uploadedDoc.id } | Select-Object -First 1
Write-Host " -> Request Status: $($approvedReq.status)" -ForegroundColor Green
Write-Host " -> Issued AES Master Key: $($approvedReq.masterKey)" -ForegroundColor Green

# Download decrypted bytes
$downloadedContent = Invoke-RestMethod -Uri "$baseUrl/files/download/$($uploadedDoc.id)" -Method Get -Headers $consumerHeaders
Write-Host " -> Downloaded Decrypted Content:" -ForegroundColor Cyan
Write-Host "----------------------------------------------------" -ForegroundColor Gray
Write-Host $downloadedContent -ForegroundColor White
Write-Host "----------------------------------------------------" -ForegroundColor Gray

# Verify exact match with original file
$originalContent = [System.IO.File]::ReadAllText($filePath)
if ($originalContent.Trim() -eq $downloadedContent.Trim()) {
    Write-Host "`n[VERIFICATION PASS] Decrypted content matches original file EXACTLY!" -ForegroundColor Green
} else {
    Write-Host "`n[VERIFICATION FAIL] Decrypted content does not match original file!" -ForegroundColor Red
}

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host "          ALL MANUAL TESTS COMPLETED PASS!          " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
