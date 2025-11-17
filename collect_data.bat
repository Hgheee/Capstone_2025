@echo off
chcp 65001 >nul
title 분실물 데이터 수집

echo ================================================
echo    분실물 데이터 자동 수집
echo ================================================
echo.

REM 백엔드 서버 확인
echo [확인] 백엔드 서버 상태 체크 중...
curl.exe -s "http://localhost:8081/actuator/health" >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ❌ 백엔드 서버가 실행되지 않았습니다!
    echo.
    echo 백엔드를 먼저 실행해주세요:
    echo   1. cd backend\capstone-backend
    echo   2. .\mvnw spring-boot:run -DskipTests
    echo.
    pause
    exit /b 1
)
echo ✅ 백엔드 서버 정상 작동 중
echo.

REM 메뉴 표시
echo ================================================
echo   수집 기간을 선택하세요
echo ================================================
echo   1. 최근 1일   (빠름, 약 100-500건)
echo   2. 최근 1주일 (보통, 약 500-2,000건)
echo   3. 최근 1개월 (느림, 약 2,000-8,000건)
echo   4. 최근 3개월 (매우 느림, 약 5,000-20,000건)
echo   5. 데이터 초기화 후 재수집
echo ================================================
echo.

set /p choice="선택 (1-5): "
echo.

REM 선택에 따라 날짜 계산
if "%choice%"=="1" (
    echo [선택] 최근 1일 데이터 수집
    for /f %%a in ('powershell -Command "Get-Date -Format yyyyMMdd"') do set endDate=%%a
    for /f %%a in ('powershell -Command "(Get-Date).AddDays(-1).ToString('yyyyMMdd')"') do set startDate=%%a
    set regions=서울,경기
    goto collect
)

if "%choice%"=="2" (
    echo [선택] 최근 1주일 데이터 수집
    for /f %%a in ('powershell -Command "Get-Date -Format yyyyMMdd"') do set endDate=%%a
    for /f %%a in ('powershell -Command "(Get-Date).AddDays(-7).ToString('yyyyMMdd')"') do set startDate=%%a
    set regions=서울,경기,부산,인천
    goto collect
)

if "%choice%"=="3" (
    echo [선택] 최근 1개월 데이터 수집
    for /f %%a in ('powershell -Command "Get-Date -Format yyyyMMdd"') do set endDate=%%a
    for /f %%a in ('powershell -Command "(Get-Date).AddDays(-30).ToString('yyyyMMdd')"') do set startDate=%%a
    set regions=서울,경기,부산,인천,대구,광주,대전
    goto collect
)

if "%choice%"=="4" (
    echo [선택] 최근 3개월 데이터 수집 (시간이 오래 걸립니다)
    for /f %%a in ('powershell -Command "Get-Date -Format yyyyMMdd"') do set endDate=%%a
    for /f %%a in ('powershell -Command "(Get-Date).AddDays(-90).ToString('yyyyMMdd')"') do set startDate=%%a
    set regions=서울,경기,부산,인천,대구,광주,대전,울산
    echo.
    set /p confirm="계속하시겠습니까? (Y/N): "
    if /i not "%confirm%"=="Y" (
        echo 취소되었습니다.
        pause
        exit /b 0
    )
    goto collect
)

if "%choice%"=="5" (
    echo [선택] 데이터 초기화 후 재수집
    echo.
    echo ⚠️ 경고: 모든 분실물 데이터가 삭제됩니다!
    set /p confirm="계속하시겠습니까? (Y/N): "
    if /i not "%confirm%"=="Y" (
        echo 취소되었습니다.
        pause
        exit /b 0
    )
    
    echo.
    set /p mysql_user=MySQL 사용자명 (기본: hogeonhee): 
    if "%mysql_user%"=="" set mysql_user=hogeonhee
    
    set /p mysql_password=MySQL 비밀번호: 
    
    echo.
    echo [1/4] 기존 데이터 삭제 중...
    mysql -u %mysql_user% -p%mysql_password% capstone_db -e "TRUNCATE TABLE lost_item;" 2>nul
    
    if %errorlevel% equ 0 (
        echo ✅ 기존 데이터 삭제 완료!
    ) else (
        echo ❌ 데이터 삭제 실패. MySQL 접속 정보를 확인하세요.
        pause
        exit /b 1
    )
    
    for /f %%a in ('powershell -Command "Get-Date -Format yyyyMMdd"') do set endDate=%%a
    for /f %%a in ('powershell -Command "(Get-Date).AddDays(-30).ToString('yyyyMMdd')"') do set startDate=%%a
    set regions=서울,경기,부산,인천
    goto collect
)

echo ❌ 잘못된 선택입니다.
pause
exit /b 1

REM 데이터 수집 시작
:collect
echo.
echo ================================================
echo   데이터 수집 시작
echo ================================================
echo 기간: %startDate% ~ %endDate%
echo 지역: %regions%
echo.

echo [1/3] LOST112 데이터 수집 중...
curl.exe -X POST "http://localhost:8081/api/admin/import/lost112-by-region?startYmd=%startDate%&endYmd=%endDate%&regions=%regions%" 2>nul

if %errorlevel% equ 0 (
    echo ✅ LOST112 수집 완료!
) else (
    echo ⚠️ LOST112 수집 중 오류 발생
)

echo.
echo [2/3] 서울교통공사 데이터 수집 중...
curl.exe -X POST "http://localhost:8081/api/admin/import/seoul" 2>nul

if %errorlevel% equ 0 (
    echo ✅ 서울교통공사 수집 완료!
) else (
    echo ⚠️ 서울교통공사 수집 중 오류 발생
)

echo.
echo [3/3] 지역 정보 업데이트 중...
curl.exe -X POST "http://localhost:8081/api/admin/update-regions" 2>nul
echo ✅ 지역 정보 업데이트 완료!

echo.
echo ================================================
echo   데이터 수집 완료!
echo ================================================
echo.

REM 통계 표시
echo 📊 현재 데이터베이스 통계:
curl.exe -s "http://localhost:8081/api/admin/region-stats" 2>nul
echo.

echo.
echo ✅ 모든 작업이 완료되었습니다!
echo.
echo 💡 다음 단계:
echo   1. 웹 브라우저에서 Ctrl+Shift+R (강력 새로고침)
echo   2. http://localhost:3000 에서 데이터 확인
echo.
pause
