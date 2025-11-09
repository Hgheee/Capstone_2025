@echo off
chcp 65001 >nul
title 백엔드 시작 및 데이터 수집

echo ================================================
echo    백엔드 시작 및 자동 데이터 수집
echo ================================================
echo.

cd /d "%~dp0.."

echo [1/3] 백엔드 서버 시작 중...
echo (백엔드가 시작되는 동안 30초 대기합니다)
echo.

start "Spring Boot Backend" cmd /c "cd backend\capstone-backend && mvnw.cmd spring-boot:run"

echo 백엔드 시작 대기 중...
timeout /t 30 /nobreak >nul

echo.
echo [2/3] 데이터 수집 시작...
echo.

cd scripts
call collect_data_simple.bat

echo.
echo [3/3] 완료!
echo.
echo 백엔드는 계속 실행 중입니다.
echo 종료하려면 백엔드 창을 닫으세요.
echo.
pause

