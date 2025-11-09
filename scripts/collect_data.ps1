# LOST112 및 서울교통공사 데이터 자동 수집 스크립트
# 사용법: 이 파일을 더블클릭하거나 PowerShell에서 실행

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   분실물 데이터 자동 수집 스크립트" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8081/api/admin"

# 백엔드 서버 확인
Write-Host "[1/4] 백엔드 서버 확인 중..." -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8081/api/health" -Method GET -TimeoutSec 5
    Write-Host "✅ 백엔드 서버 정상 작동 중" -ForegroundColor Green
} catch {
    Write-Host "❌ 백엔드 서버가 실행되지 않았습니다!" -ForegroundColor Red
    Write-Host "   백엔드를 먼저 실행해주세요: cd backend/capstone-backend && ./mvnw spring-boot:run" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "계속하려면 Enter 키를 누르세요"
    exit 1
}

Write-Host ""

# 수집할 데이터 선택
Write-Host "수집할 데이터를 선택하세요:" -ForegroundColor Cyan
Write-Host "  1. LOST112 (경찰청 유실물) - 최근 7일" -ForegroundColor White
Write-Host "  2. LOST112 (경찰청 유실물) - 최근 30일" -ForegroundColor White
Write-Host "  3. LOST112 (지역별) - 서울/경기/부산/인천" -ForegroundColor White
Write-Host "  4. 서울교통공사 (지하철 분실물)" -ForegroundColor White
Write-Host "  5. 전체 수집 (LOST112 + 서울교통공사)" -ForegroundColor White
Write-Host ""
$choice = Read-Host "선택 (1-5)"

Write-Host ""

switch ($choice) {
    "1" {
        Write-Host "[2/4] LOST112 데이터 수집 중 (최근 7일)..." -ForegroundColor Yellow
        $startDate = (Get-Date).AddDays(-7).ToString("yyyyMMdd")
        $endDate = (Get-Date).ToString("yyyyMMdd")
        
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/lost112-by-region?startYmd=$startDate&endYmd=$endDate&regions=서울,경기,부산,인천" -Method POST -TimeoutSec 300
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ LOST112 수집 완료!" -ForegroundColor Green
            Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
        } catch {
            Write-Host "❌ LOST112 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    "2" {
        Write-Host "[2/4] LOST112 데이터 수집 중 (최근 30일)..." -ForegroundColor Yellow
        $startDate = (Get-Date).AddDays(-30).ToString("yyyyMMdd")
        $endDate = (Get-Date).ToString("yyyyMMdd")
        
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/lost112-by-region?startYmd=$startDate&endYmd=$endDate&regions=서울,경기,부산,인천,대구,광주,대전" -Method POST -TimeoutSec 600
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ LOST112 수집 완료!" -ForegroundColor Green
            Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
        } catch {
            Write-Host "❌ LOST112 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    "3" {
        Write-Host "[2/4] LOST112 지역별 데이터 수집 중..." -ForegroundColor Yellow
        $startDate = (Get-Date).AddDays(-14).ToString("yyyyMMdd")
        $endDate = (Get-Date).ToString("yyyyMMdd")
        
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/lost112-by-region?startYmd=$startDate&endYmd=$endDate&regions=서울,경기,부산,인천" -Method POST -TimeoutSec 600
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ LOST112 지역별 수집 완료!" -ForegroundColor Green
            Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
        } catch {
            Write-Host "❌ LOST112 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    "4" {
        Write-Host "[2/4] 서울교통공사 데이터 수집 중..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/seoul" -Method POST -TimeoutSec 300
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ 서울교통공사 수집 완료!" -ForegroundColor Green
            if ($result.data.totalFetched) {
                Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
            }
        } catch {
            Write-Host "❌ 서울교통공사 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    "5" {
        Write-Host "[2/4] LOST112 데이터 수집 중..." -ForegroundColor Yellow
        $startDate = (Get-Date).AddDays(-14).ToString("yyyyMMdd")
        $endDate = (Get-Date).ToString("yyyyMMdd")
        
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/lost112-by-region?startYmd=$startDate&endYmd=$endDate&regions=서울,경기,부산,인천" -Method POST -TimeoutSec 600
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ LOST112 수집 완료!" -ForegroundColor Green
            Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
        } catch {
            Write-Host "❌ LOST112 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
        
        Write-Host ""
        Write-Host "[3/4] 서울교통공사 데이터 수집 중..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri "$baseUrl/import/seoul" -Method POST -TimeoutSec 300
            $result = $response.Content | ConvertFrom-Json
            Write-Host "✅ 서울교통공사 수집 완료!" -ForegroundColor Green
            if ($result.data.totalFetched) {
                Write-Host "   수집된 데이터: $($result.data.totalFetched)건" -ForegroundColor White
            }
        } catch {
            Write-Host "❌ 서울교통공사 수집 실패: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    default {
        Write-Host "❌ 잘못된 선택입니다." -ForegroundColor Red
        Read-Host "계속하려면 Enter 키를 누르세요"
        exit 1
    }
}

Write-Host ""
Write-Host "[4/4] 지역 정보 업데이트 중..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/update-regions" -Method POST -TimeoutSec 300
    Write-Host "✅ 지역 정보 업데이트 완료!" -ForegroundColor Green
} catch {
    Write-Host "⚠️ 지역 정보 업데이트 실패 (무시 가능): $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   데이터 수집 완료!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 통계 확인
try {
    $stats = Invoke-WebRequest -Uri "$baseUrl/region-stats" -Method GET
    $statsData = $stats.Content | ConvertFrom-Json
    Write-Host "📊 현재 데이터베이스 통계:" -ForegroundColor Cyan
    Write-Host "   전체 분실물: $($statsData.data.totalCount)건" -ForegroundColor White
    Write-Host "   지역 정보 있음: $($statsData.data.withRegion)건" -ForegroundColor White
    Write-Host "   지역 정보 커버리지: $($statsData.data.regionCoverage)%" -ForegroundColor White
} catch {
    Write-Host "⚠️ 통계 조회 실패" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "완료! Enter 키를 눌러 종료하세요"

