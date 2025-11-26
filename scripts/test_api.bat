@echo off
chcp 65001 >nul
title API 테스트 및 진단

echo ================================================
echo    API 연결 테스트 및 진단
echo ================================================
echo.

echo [1/5] 백엔드 서버 상태 확인...
curl.exe -s "http://localhost:8081/actuator/health" 2>nul

if %errorlevel% neq 0 (
    echo ❌ 백엔드 서버가 응답하지 않습니다!
    echo.
    echo 백엔드를 실행해주세요:
    echo   cd backend\capstone-backend
    echo   .\mvnw spring-boot:run -DskipTests
    echo.
    pause
    exit /b 1
)

echo ✅ 백엔드 서버 정상
echo.

echo [2/5] 데이터베이스 연결 확인...
curl.exe -s "http://localhost:8081/api/admin/region-stats" 2>nul

if %errorlevel% equ 0 (
    echo ✅ 데이터베이스 연결 정상
) else (
    echo ❌ 데이터베이스 연결 실패
)

echo.
echo [3/5] 현재 데이터 개수 확인...
curl.exe -s "http://localhost:8081/api/lost-items?page=0&size=1" 2>nul
echo.

echo [4/5] LOST112 API 테스트 (기본 수집)...
echo 테스트 중... (30초 소요)
curl.exe -X POST "http://localhost:8081/api/admin/import/lost112" -w "\n응답 코드: %%{http_code}\n" 2>nul
echo.

echo [5/5] 서울교통공사 API 테스트...
echo 테스트 중... (10초 소요)
curl.exe -X POST "http://localhost:8081/api/admin/import/seoul" -w "\n응답 코드: %%{http_code}\n" 2>nul
echo.

echo ================================================
echo    테스트 완료
echo ================================================
echo.

echo 📊 최종 데이터 개수:
curl.exe -s "http://localhost:8081/api/admin/region-stats" 2>nul
echo.

echo.
echo 💡 만약 수집된 데이터가 0건이라면:
echo   1. application.yml 파일에서 API 키 확인
echo   2. 인터넷 연결 확인
echo   3. 방화벽 설정 확인
echo   4. 백엔드 로그 확인 (터미널)
echo.
pause







