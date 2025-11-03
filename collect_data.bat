@echo off
echo ============================================
echo LOST112 분실물 데이터 수집 스크립트
echo ============================================
echo.

REM 현재 디렉토리 확인
cd /d "%~dp0"

REM Python 확인
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [오류] Python이 설치되어 있지 않습니다.
    echo Python 3.8 이상을 설치해주세요.
    pause
    exit /b 1
)

REM 필요한 패키지 설치
echo [1/4] Python 패키지 설치 중...
pip install --quiet pandas requests python-dotenv mysql-connector-python

REM .env 파일 확인
if not exist "backend\capstone-backend\data\.env" (
    echo [오류] .env 파일이 없습니다!
    echo backend\capstone-backend\data\.env 파일을 생성해주세요.
    echo.
    echo 예시:
    echo LOST112_SERVICE_KEY=your_api_key_here
    echo DB_HOST=localhost
    echo DB_PORT=3306
    echo DB_NAME=capstonedb
    echo DB_USER=hogeonhee
    echo DB_PASSWORD=0316
    pause
    exit /b 1
)

echo [2/4] 환경 설정 확인 완료
echo.

REM 사용자에게 옵션 선택
echo 수집할 데이터 기간을 선택하세요:
echo 1. 최근 1일 (빠름, 약 100-500개)
echo 2. 최근 7일 (보통, 약 500-2000개)
echo 3. 최근 30일 (느림, 약 2000-5000개)
echo 4. 사용자 지정
echo.
set /p choice="선택 (1-4): "

if "%choice%"=="1" (
    set days=1
    set maxpages=10
) else if "%choice%"=="2" (
    set days=7
    set maxpages=20
) else if "%choice%"=="3" (
    set days=30
    set maxpages=50
) else if "%choice%"=="4" (
    set /p days="일 수 입력: "
    set /p maxpages="최대 페이지 수 입력: "
) else (
    echo 잘못된 선택입니다.
    pause
    exit /b 1
)

echo.
echo [3/4] 데이터 수집 시작...
echo 기간: 최근 %days%일
echo 최대 페이지: %maxpages%
echo.

cd backend\capstone-backend\data
python lost112_collect_and_sync_fast.py --days %days% --rows 100 --max-pages %maxpages%

if %errorlevel% neq 0 (
    echo.
    echo [오류] 데이터 수집 중 오류가 발생했습니다.
    echo 로그를 확인해주세요.
    cd ..\..\..\
    pause
    exit /b 1
)

cd ..\..\..\

echo.
echo [4/4] 데이터 수집 완료!
echo.
echo 다음 단계:
echo 1. 백엔드 실행: cd backend\capstone-backend ^&^& mvnw.cmd spring-boot:run
echo 2. 브라우저 접속: http://localhost:5173
echo.
pause

