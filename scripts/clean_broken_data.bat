@echo off
chcp 65001 >nul
title 한글 깨진 데이터 정리

echo ================================================
echo    한글 깨진 데이터 확인 및 삭제
echo ================================================
echo.

echo [1/3] MySQL 접속 중...
echo 비밀번호를 입력하세요:
echo.

mysql -u hogeonhee -p capstone_db < scripts\fix_encoding_issue.sql

if %errorlevel% neq 0 (
    echo ❌ MySQL 실행 실패
    echo.
    echo 수동으로 실행하려면:
    echo mysql -u hogeonhee -p capstone_db
    echo source scripts/fix_encoding_issue.sql;
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ 깨진 데이터 삭제 완료!
echo.

echo [2/3] 백엔드 API로 지역 정보 업데이트 중...
curl.exe -X POST "http://localhost:8081/api/admin/update-regions" 2>nul
echo ✅ 지역 정보 업데이트 완료!
echo.

echo [3/3] 새로운 데이터 수집 중...
call scripts\collect_data_simple.bat

echo.
echo ================================================
echo    모든 작업 완료!
echo ================================================
echo.
pause

