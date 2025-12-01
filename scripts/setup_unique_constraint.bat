@echo off
chcp 65001 >nul
title UNIQUE 제약 조건 추가

echo ================================================
echo    external_id에 UNIQUE 제약 조건 추가
echo ================================================
echo.

echo MySQL 사용자명 (기본: hogeonhee):
set /p mysql_user=
if "%mysql_user%"=="" set mysql_user=hogeonhee

echo MySQL 비밀번호:
set /p mysql_password=

echo.
echo [1/4] 기존 중복 데이터 확인 중...
echo.

mysql -u %mysql_user% -p%mysql_password% capstone_db -e "SELECT external_id, COUNT(*) as duplicate_count, datasource FROM lost_item WHERE external_id IS NOT NULL GROUP BY external_id, datasource HAVING COUNT(*) > 1;" 2>nul

if %errorlevel% neq 0 (
    echo ❌ MySQL 접속 실패. 사용자명과 비밀번호를 확인하세요.
    pause
    exit /b 1
)

echo.
echo [2/4] 중복 데이터 정리 중...
echo.

mysql -u %mysql_user% -p%mysql_password% capstone_db -e "DELETE l1 FROM lost_item l1 INNER JOIN lost_item l2 WHERE l1.external_id = l2.external_id AND l1.id > l2.id AND l1.external_id IS NOT NULL;" 2>nul

if %errorlevel% equ 0 (
    echo ✅ 중복 데이터 정리 완료!
) else (
    echo ⚠️  중복 데이터가 없거나 정리 불필요
)

echo.
echo [3/4] UNIQUE 제약 조건 추가 중...
echo.

mysql -u %mysql_user% -p%mysql_password% capstone_db -e "ALTER TABLE lost_item ADD UNIQUE INDEX idx_unique_external_id (external_id);" 2>nul

if %errorlevel% equ 0 (
    echo ✅ UNIQUE 제약 조건 추가 완료!
) else (
    echo ⚠️  UNIQUE 제약 조건이 이미 존재하거나 추가 실패
    mysql -u %mysql_user% -p%mysql_password% capstone_db -e "SHOW INDEX FROM lost_item WHERE Key_name = 'idx_unique_external_id';" 2>nul
)

echo.
echo [4/4] 결과 확인 중...
echo.

mysql -u %mysql_user% -p%mysql_password% capstone_db -e "SHOW INDEX FROM lost_item WHERE Key_name = 'idx_unique_external_id';" 2>nul

echo.
echo ================================================
echo    UNIQUE 제약 조건 설정 완료!
echo ================================================
echo.
echo 이제 중복 데이터가 자동으로 방지됩니다.
echo.
pause










