@echo off
chcp 65001 >nul
title 분실물 데이터 자동 수집

echo ================================================
echo    분실물 데이터 자동 수집 (간편 버전)
echo ================================================
echo.

echo [1/3] LOST112 데이터 수집 중...
curl.exe -X POST "http://localhost:8081/api/admin/import/lost112-by-region?regions=서울,경기,부산,인천" 2>nul
if %errorlevel% neq 0 (
    echo ❌ LOST112 수집 실패 - 백엔드가 실행 중인지 확인하세요
    goto error
)
echo ✅ LOST112 수집 완료!
echo.

echo [2/3] 서울교통공사 데이터 수집 중...
curl.exe -X POST "http://localhost:8081/api/admin/import/seoul" 2>nul
if %errorlevel% neq 0 (
    echo ❌ 서울교통공사 수집 실패
    goto error
)
echo ✅ 서울교통공사 수집 완료!
echo.

echo [3/3] 지역 정보 업데이트 중...
curl.exe -X POST "http://localhost:8081/api/admin/update-regions" 2>nul
echo ✅ 지역 정보 업데이트 완료!
echo.

echo ================================================
echo    모든 데이터 수집 완료!
echo ================================================
echo.
pause
exit /b 0

:error
echo.
echo 백엔드 서버를 먼저 실행해주세요:
echo cd backend\capstone-backend
echo .\mvnw spring-boot:run
echo.
pause
exit /b 1

