# Helma Bank API - Smart Validation Tests (PowerShell)
# Exécutez ce script pour tester les 3 modes de périodicité
# Usage: .\test-smart-validation.ps1

$BaseUrl = "http://localhost:8082/helma/transactions/add/1"
$Headers = @{
    "Content-Type" = "application/json"
    "Accept" = "*/*"
}

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║ Helma Bank API - Smart Validation Tests                       ║" -ForegroundColor Cyan
Write-Host "║ Test des 3 modes de periodicity: NOW, SCHEDULED, PERMANENT    ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

# ─────────────────────────────────────────────────────────────────────
# TEST 1: NOW (IMMÉDIAT)
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "TEST 1️⃣: NOW (Immédiat - aucune date requise)" -ForegroundColor Green
Write-Host "="*70

$payload1 = @{
    "beneficiaryName" = "Test NOW"
    "beneficiaryRib" = "12233455TNZ"
    "amount" = 100
    "type" = "EXTERNAL"
    "category" = "TEST"
    "description" = "Transaction immédiate"
    "periodicity" = "NOW"
} | ConvertTo-Json

Write-Host "`n📤 Request:" -ForegroundColor Yellow
Write-Host $payload1 -ForegroundColor Gray

try {
    Write-Host "`n⏳ Envoi..." -ForegroundColor Cyan
    $response1 = Invoke-RestMethod -Uri $BaseUrl -Method Post -Headers $Headers -Body $payload1
    
    Write-Host "`n✅ Response (200 OK):" -ForegroundColor Green
    Write-Host ($response1 | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    Write-Host "`n✔️ Vérifications:" -ForegroundColor Green
    Write-Host "  ✓ status = CONFIRMED: $($response1.status -eq 'CONFIRMED')" -ForegroundColor Green
    Write-Host "  ✓ periodicity = NOW: $($response1.periodicity -eq 'NOW')" -ForegroundColor Green
    Write-Host "  ✓ scheduledDate = null: $($response1.scheduledDate -eq $null)" -ForegroundColor Green
    Write-Host "  ✓ nextExecutionDate = null: $($response1.nextExecutionDate -eq $null)" -ForegroundColor Green
} catch {
    Write-Host "`n❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}


# ─────────────────────────────────────────────────────────────────────
# TEST 2: SCHEDULED (PROGRAMMÉ)
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "TEST 2️⃣: SCHEDULED (Programmé - date REQUISE)" -ForegroundColor Green
Write-Host "="*70

$futureDate = (Get-Date).AddDays(5).ToString("yyyy-MM-ddTHH:mm:ss")

$payload2 = @{
    "beneficiaryName" = "Test SCHEDULED"
    "beneficiaryRib" = "98765432TNZ"
    "amount" = 200
    "type" = "EXTERNAL"
    "category" = "TEST"
    "description" = "Transaction programmée"
    "periodicity" = "SCHEDULED"
    "scheduledDate" = $futureDate
} | ConvertTo-Json

Write-Host "`n📤 Request (date future: $futureDate):" -ForegroundColor Yellow
Write-Host $payload2 -ForegroundColor Gray

try {
    Write-Host "`n⏳ Envoi..." -ForegroundColor Cyan
    $response2 = Invoke-RestMethod -Uri $BaseUrl -Method Post -Headers $Headers -Body $payload2
    
    Write-Host "`n✅ Response (200 OK):" -ForegroundColor Green
    Write-Host ($response2 | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    Write-Host "`n✔️ Vérifications:" -ForegroundColor Green
    Write-Host "  ✓ status = PENDING: $($response2.status -eq 'PENDING')" -ForegroundColor Green
    Write-Host "  ✓ periodicity = SCHEDULED: $($response2.periodicity -eq 'SCHEDULED')" -ForegroundColor Green
    Write-Host "  ✓ scheduledDate = future: $($response2.scheduledDate -ne $null)" -ForegroundColor Green
    Write-Host "  ✓ nextExecutionDate = null: $($response2.nextExecutionDate -eq $null)" -ForegroundColor Green
} catch {
    Write-Host "`n❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}


# ─────────────────────────────────────────────────────────────────────
# TEST 3: SCHEDULED SANS DATE (ERREUR ATTENDUE)
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "TEST 3️⃣: SCHEDULED SANS DATE (Erreur attendue 400)" -ForegroundColor Red
Write-Host "="*70

$payload3 = @{
    "beneficiaryName" = "Test ERROR"
    "beneficiaryRib" = "98765432TNZ"
    "amount" = 200
    "type" = "EXTERNAL"
    "periodicity" = "SCHEDULED"
} | ConvertTo-Json

Write-Host "`n📤 Request (SANS scheduledDate):" -ForegroundColor Yellow
Write-Host $payload3 -ForegroundColor Gray

try {
    Write-Host "`n⏳ Envoi..." -ForegroundColor Cyan
    $response3 = Invoke-RestMethod -Uri $BaseUrl -Method Post -Headers $Headers -Body $payload3
    Write-Host "`n⚠️ Unexpected success:" -ForegroundColor Yellow
} catch {
    Write-Host "`n❌ Expected Error (400):" -ForegroundColor Red
    $errorMsg = $_.Exception.Response.Content | ConvertFrom-Json
    Write-Host ($errorMsg | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    Write-Host "`n✔️ Message contient 'SCHEDULED'?: $($errorMsg.error -match 'SCHEDULED')" -ForegroundColor Green
    Write-Host "✔️ Message contient 'scheduledDate'?: $($errorMsg.error -match 'scheduledDate')" -ForegroundColor Green
}


# ─────────────────────────────────────────────────────────────────────
# TEST 4: PERMANENT (OPTIONNEL, DÉFAUT AUTO +30j)
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "TEST 4️⃣: PERMANENT (Récurrent - date OPTIONNELLE)" -ForegroundColor Green
Write-Host "="*70

$payload4 = @{
    "beneficiaryName" = "Test PERMANENT"
    "beneficiaryRib" = "11111111TNZ"
    "amount" = 50
    "type" = "EXTERNAL"
    "category" = "TEST"
    "description" = "Transaction permanente (sans date)"
    "periodicity" = "PERMANENT"
} | ConvertTo-Json

Write-Host "`n📤 Request (SANS nextExecutionDate):" -ForegroundColor Yellow
Write-Host $payload4 -ForegroundColor Gray

try {
    Write-Host "`n⏳ Envoi..." -ForegroundColor Cyan
    $response4 = Invoke-RestMethod -Uri $BaseUrl -Method Post -Headers $Headers -Body $payload4
    
    Write-Host "`n✅ Response (200 OK):" -ForegroundColor Green
    Write-Host ($response4 | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    Write-Host "`n✔️ Vérifications:" -ForegroundColor Green
    Write-Host "  ✓ status = CONFIRMED: $($response4.status -eq 'CONFIRMED')" -ForegroundColor Green
    Write-Host "  ✓ periodicity = PERMANENT: $($response4.periodicity -eq 'PERMANENT')" -ForegroundColor Green
    Write-Host "  ✓ nextExecutionDate auto-défini: $($response4.nextExecutionDate -ne $null)" -ForegroundColor Green
    Write-Host "  ✓ scheduledDate = null: $($response4.scheduledDate -eq $null)" -ForegroundColor Green
    
    if ($response4.nextExecutionDate) {
        Write-Host "  📅 nextExecutionDate: $($response4.nextExecutionDate)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "`n❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}


# ─────────────────────────────────────────────────────────────────────
# TEST 5: PERMANENT AVEC DATE FUTURE
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "TEST 5️⃣: PERMANENT AVEC DATE (Récurrent - date fournie)" -ForegroundColor Green
Write-Host "="*70

$futureDatePerm = (Get-Date).AddDays(15).ToString("yyyy-MM-ddTHH:mm:ss")

$payload5 = @{
    "beneficiaryName" = "Test PERMANENT Custom"
    "beneficiaryRib" = "22222222TNZ"
    "amount" = 75
    "type" = "EXTERNAL"
    "category" = "TEST"
    "description" = "Transaction permanente (avec date)"
    "periodicity" = "PERMANENT"
    "nextExecutionDate" = $futureDatePerm
} | ConvertTo-Json

Write-Host "`n📤 Request (date future: $futureDatePerm):" -ForegroundColor Yellow
Write-Host $payload5 -ForegroundColor Gray

try {
    Write-Host "`n⏳ Envoi..." -ForegroundColor Cyan
    $response5 = Invoke-RestMethod -Uri $BaseUrl -Method Post -Headers $Headers -Body $payload5
    
    Write-Host "`n✅ Response (200 OK):" -ForegroundColor Green
    Write-Host ($response5 | ConvertTo-Json -Depth 3) -ForegroundColor Gray
    
    Write-Host "`n✔️ Vérifications:" -ForegroundColor Green
    Write-Host "  ✓ status = CONFIRMED: $($response5.status -eq 'CONFIRMED')" -ForegroundColor Green
    Write-Host "  ✓ periodicity = PERMANENT: $($response5.periodicity -eq 'PERMANENT')" -ForegroundColor Green
    Write-Host "  ✓ nextExecutionDate = fourni: $($response5.nextExecutionDate -ne $null)" -ForegroundColor Green
    Write-Host "  ✓ scheduledDate = null: $($response5.scheduledDate -eq $null)" -ForegroundColor Green
} catch {
    Write-Host "`n❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}


# ─────────────────────────────────────────────────────────────────────
# RÉSUMÉ
# ─────────────────────────────────────────────────────────────────────

Write-Host "`n" + "="*70
Write-Host "RÉSUMÉ DES TESTS" -ForegroundColor Cyan
Write-Host "="*70
Write-Host "`n✅ Tous les tests doivent montrer:" -ForegroundColor Green
Write-Host "  1. NOW: status=CONFIRMED, pas de dates"
Write-Host "  2. SCHEDULED: status=PENDING, scheduledDate présente"
Write-Host "  3. SCHEDULED sans date: error 400 avec message"
Write-Host "  4. PERMANENT: status=CONFIRMED, nextExecutionDate auto-calculée"
Write-Host "  5. PERMANENT custom: status=CONFIRMED, nextExecutionDate fournie"

Write-Host "`n🛠️ En cas de problème:" -ForegroundColor Yellow
Write-Host "  1. Vérifiez que l'app est en cours: http://localhost:8082"
Write-Host "  2. Vérifiez que les comptes existent (IDs 1 et RIBs)"
Write-Host "  3. Consultez les logs de l'app pour plus de détails"
Write-Host "  4. Vérifiez le document API_SMART_VALIDATION.md"

Write-Host "`n" + "="*70 + "`n" -ForegroundColor Cyan
