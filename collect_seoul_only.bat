@echo off
chcp 65001 >nul
title 서울교통공사 분실물 데이터 수집

echo ================================================
echo    서울교통공사 분실물 데이터 수집
echo ================================================
echo.

REM 백엔드 서버 확인
echo [1/4] 백엔드 서버 상태 체크 중...
curl.exe -s "http://localhost:8081/actuator/health" >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ❌ 백엔드 서버가 실행되지 않았습니다!
    echo.
    echo 백엔드를 먼저 실행해주세요:
    echo   cd backend\capstone-backend
    echo   .\mvnw spring-boot:run -DskipTests
    echo.
    pause
    exit /b 1
)
echo ✅ 백엔드 서버 정상 작동 중
echo.

REM LOST112 데이터 삭제 여부 확인
echo ================================================
echo [2/4] 기존 LOST112 데이터 삭제
echo ================================================
echo.
echo ⚠️ LOST112 데이터를 모두 삭제하시겠습니까?
echo    (서울교통공사 데이터는 유지됩니다)
echo.
set /p delete_confirm="삭제하려면 Y를 입력하세요 (Y/N): "

if /i "%delete_confirm%"=="Y" (
    echo.
    echo MySQL 비밀번호를 입력하세요:
    mysql -u hogeonhee -p capstone_db -e "DELETE FROM lost_item WHERE datasource = 'LOST112';"
    if %errorlevel% equ 0 (
        echo ✅ LOST112 데이터 삭제 완료!
    ) else (
        echo ⚠️ MySQL 삭제 실패 (계속 진행합니다)
    )
) else (
    echo ⏭️ LOST112 데이터 삭제 건너뜀
)
echo.

REM 서울교통공사 데이터 수집
echo ================================================
echo [3/4] 서울교통공사 데이터 수집 중...
echo ================================================
echo.
echo ⏳ 데이터 수집 중입니다...
echo    (예상 시간: 5-10분, 최대 100,000건 수집 가능)
echo.

curl.exe -X POST "http://localhost:8081/api/admin/seoul/import" --max-time 3600 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ❌ 서울교통공사 수집 실패!
    echo.
    echo 백엔드 로그를 확인하세요:
    echo   - API 키가 올바른지 확인 (data/.env 파일)
    echo   - 네트워크 연결 상태 확인
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ 서울교통공사 수집 완료!
echo.

REM 지역 정보 업데이트
echo ================================================
echo [4/4] 지역 정보 업데이트 중...
echo ================================================
curl.exe -X POST "http://localhost:8081/api/admin/update-regions" --max-time 600 2>nul
if %errorlevel% neq 0 (
    echo ⚠️ 지역 정보 업데이트 실패 (무시 가능)
) else (
    echo ✅ 지역 정보 업데이트 완료!
)
echo.

REM 통계 확인
echo ================================================
echo    데이터 수집 완료!
echo ================================================
echo.
echo 📊 현재 데이터베이스 통계:
curl.exe -s "http://localhost:8081/api/admin/region-stats" 2>nul
echo.
echo.

echo ✅ 모든 작업 완료!
echo.
echo 웹 브라우저에서 확인하세요:
echo   http://localhost:3000
echo.
echo Ctrl+Shift+R 로 새로고침하면 최신 데이터가 표시됩니다.
echo.

pause
exit /b 0

