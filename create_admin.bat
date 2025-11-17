@echo off
chcp 65001 > nul
echo ====================================
echo    관리자 계정 생성 스크립트
echo ====================================
echo.
echo 관리자 계정 정보:
echo - 이메일: admin@admin.com
echo - 비밀번호: Snow0316!
echo.
echo MySQL 접속 정보를 입력하세요.
echo.

set /p DB_USER="MySQL 사용자명 (기본: root): " || set DB_USER=root
set /p DB_PASSWORD="MySQL 비밀번호: "
set /p DB_NAME="데이터베이스 이름 (기본: lost_and_found_db): " || set DB_NAME=lost_and_found_db

echo.
echo 관리자 계정을 생성하시겠습니까? (Y/N)
set /p CONFIRM=

if /i "%CONFIRM%" neq "Y" (
    echo 작업이 취소되었습니다.
    pause
    exit /b
)

echo.
echo 관리자 계정 생성 중...
echo.

mysql -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% < create_admin_account.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✓ 관리자 계정이 성공적으로 생성되었습니다!
    echo.
    echo 로그인 정보:
    echo - 이메일: admin@admin.com
    echo - 비밀번호: Snow0316!
    echo.
) else (
    echo.
    echo ✗ 관리자 계정 생성에 실패했습니다.
    echo MySQL 연결 정보를 확인하세요.
    echo.
)

pause



