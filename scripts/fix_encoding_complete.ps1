# 한글 깨짐 문제 완전 해결 스크립트
# UTF-8 인코딩으로 실행

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   한글 깨짐 문제 해결 스크립트" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# MySQL 접속 정보
$mysqlUser = "hogeonhee"
$mysqlDb = "capstone_db"

Write-Host "[1/5] 깨진 데이터 확인 중..." -ForegroundColor Yellow
Write-Host "MySQL 비밀번호를 입력하세요:" -ForegroundColor White
$mysqlPassword = Read-Host -AsSecureString
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($mysqlPassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

Write-Host ""

# 깨진 데이터 개수 확인
$checkQuery = @"
SELECT 
    datasource,
    status,
    COUNT(*) as broken_count
FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%'
GROUP BY datasource, status;
"@

Write-Host "깨진 데이터 개수를 확인 중..." -ForegroundColor Yellow
Write-Host $checkQuery | mysql -u $mysqlUser -p$plainPassword $mysqlDb 2>$null

Write-Host ""
Write-Host "위 데이터를 삭제하시겠습니까? (Y/N)" -ForegroundColor Yellow
$confirm = Read-Host

if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "작업이 취소되었습니다." -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "[2/5] 백업 생성 중..." -ForegroundColor Yellow
$backupQuery = @"
CREATE TABLE IF NOT EXISTS lost_item_backup_$(Get-Date -Format 'yyyyMMdd') AS
SELECT * FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%';
"@

Write-Host $backupQuery | mysql -u $mysqlUser -p$plainPassword $mysqlDb 2>$null
Write-Host "✅ 백업 완료!" -ForegroundColor Green

Write-Host ""
Write-Host "[3/5] 깨진 데이터 삭제 중..." -ForegroundColor Yellow
$deleteQuery = @"
DELETE FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%';
"@

Write-Host $deleteQuery | mysql -u $mysqlUser -p$plainPassword $mysqlDb 2>$null
Write-Host "✅ 깨진 데이터 삭제 완료!" -ForegroundColor Green

Write-Host ""
Write-Host "[4/5] 백엔드 재시작이 필요합니다" -ForegroundColor Yellow
Write-Host "인코딩 설정이 수정되었으므로 백엔드를 다시 시작해주세요." -ForegroundColor Yellow
Write-Host "백엔드를 재시작하셨나요? (Y/N)" -ForegroundColor White
$restart = Read-Host

if ($restart -eq "Y" -or $restart -eq "y") {
    Write-Host ""
    Write-Host "[5/5] 새로운 데이터 수집 중..." -ForegroundColor Yellow
    
    try {
        Start-Sleep -Seconds 3
        Invoke-WebRequest -Uri "http://localhost:8081/api/admin/import/lost112-by-region?regions=서울,경기" -Method POST -TimeoutSec 300
        Write-Host "✅ LOST112 수집 완료!" -ForegroundColor Green
        
        Invoke-WebRequest -Uri "http://localhost:8081/api/admin/import/seoul" -Method POST -TimeoutSec 300
        Write-Host "✅ 서울교통공사 수집 완료!" -ForegroundColor Green
        
        Invoke-WebRequest -Uri "http://localhost:8081/api/admin/update-regions" -Method POST -TimeoutSec 300
        Write-Host "✅ 지역 정보 업데이트 완료!" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ 데이터 수집 실패: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "백엔드가 실행 중인지 확인해주세요." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   모든 작업 완료!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 현재 상태 확인:" -ForegroundColor Cyan
Write-Host "   http://localhost:8081/api/admin/region-stats" -ForegroundColor White
Write-Host ""
Read-Host "Enter 키를 눌러 종료하세요"

