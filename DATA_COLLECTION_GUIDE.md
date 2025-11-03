# 실제 분실물 데이터 수집 가이드

## 🎯 목표
샘플 데이터 대신 **실제 LOST112와 서울시 분실물 데이터**를 수집해서 데이터베이스에 저장합니다.

---

## 📋 준비사항

### 1. API 키 준비 ✅ (완료)
- LOST112 API 키
- 서울시 공공 API 키

### 2. Python 환경
```bash
# Python 3.8 이상
python --version

# 필요한 패키지 설치
pip install pandas requests python-dotenv mysql-connector-python
```

---

## 🚀 방법 1: Python 스크립트 직접 실행 (추천)

### 1️⃣ 환경 변수 파일 생성

`backend\capstone-backend\data\.env` 파일 생성:

```env
# LOST112 API
LOST112_SERVICE_KEY=your_lost112_api_key_here

# MySQL 설정
DB_HOST=localhost
DB_PORT=3306
DB_NAME=capstonedb
DB_USER=hogeonhee
DB_PASSWORD=0316

# 서울시 API (옵션)
SEOUL_API_KEY=your_seoul_api_key_here
```

### 2️⃣ Python 스크립트 실행

```bash
cd backend\capstone-backend\data

# 최근 7일 데이터 수집 (100개/페이지, 최대 20페이지)
python lost112_collect_and_sync_fast.py --days 7 --rows 100 --max-pages 20

# 특정 기간 지정
python lost112_collect_and_sync_fast.py --start 20250101 --end 20250115 --rows 100 --max-pages 50

# 더 많은 데이터 (주의: 시간이 오래 걸림)
python lost112_collect_and_sync_fast.py --days 30 --rows 100 --max-pages 100
```

**결과**: 데이터가 `capstonedb.lost_item` 테이블에 자동으로 저장됩니다!

### 3️⃣ 수집된 데이터 확인

```bash
mysql -u hogeonhee -p capstonedb

# MySQL에서
SELECT COUNT(*) FROM lost_item WHERE datasource = 'LOST112';
SELECT id, title, category, location, found_date FROM lost_item WHERE datasource = 'LOST112' LIMIT 10;
```

---

## 🚀 방법 2: 백엔드 API 사용

백엔드가 실행 중일 때 API를 통해 데이터를 수집할 수 있습니다.

### 1️⃣ 백엔드 실행

```bash
cd backend\capstone-backend
.\mvnw.cmd spring-boot:run
```

### 2️⃣ API로 데이터 수집 (Swagger 사용)

브라우저에서:
```
http://localhost:8080/swagger-ui/index.html
```

**Admin Controller** → **POST /api/admin/lost112/import-python**

Request Body:
```json
{
  "startYmd": "20250101",
  "endYmd": "20250115",
  "regionCode": "01",
  "rows": 100,
  "maxPages": 20
}
```

**Execute** 버튼 클릭!

### 3️⃣ 또는 curl로 실행

```bash
curl -X POST "http://localhost:8080/api/admin/lost112/import-python" ^
  -H "Content-Type: application/json" ^
  -d "{\"startYmd\":\"20250101\",\"endYmd\":\"20250115\",\"regionCode\":\"01\",\"rows\":100,\"maxPages\":20}"
```

---

## 📸 이미지 처리

### 현재 상태
LOST112 API는 이미지 URL을 제공합니다. 백엔드에서 자동으로 `imagePath` 필드에 저장됩니다.

### 이미지 다운로드 (옵션)

이미지를 로컬에 저장하려면:

**1. 이미지 다운로드 스크립트 생성**

`backend\capstone-backend\data\download_images.py`:

```python
import os
import requests
import mysql.connector
from pathlib import Path

# MySQL 연결
conn = mysql.connector.connect(
    host='localhost',
    user='hogeonhee',
    password='0316',
    database='capstonedb'
)

cursor = conn.cursor()

# 이미지 저장 경로
IMAGES_DIR = Path("../../uploads/lost_items")
IMAGES_DIR.mkdir(parents=True, exist_ok=True)

# 이미지 URL이 있는 항목 가져오기
cursor.execute("SELECT id, image_path FROM lost_item WHERE image_path IS NOT NULL AND image_path != ''")

for item_id, image_url in cursor:
    if not image_url or not image_url.startswith('http'):
        continue
    
    try:
        # 이미지 다운로드
        response = requests.get(image_url, timeout=10)
        if response.status_code == 200:
            # 파일명 생성
            ext = image_url.split('.')[-1].split('?')[0]
            filename = f"item_{item_id}.{ext}"
            filepath = IMAGES_DIR / filename
            
            # 저장
            with open(filepath, 'wb') as f:
                f.write(response.content)
            
            # DB 업데이트 (로컬 경로로)
            local_path = f"/uploads/lost_items/{filename}"
            cursor.execute("UPDATE lost_item SET image_path = %s WHERE id = %s", (local_path, item_id))
            conn.commit()
            
            print(f"Downloaded: {filename}")
    except Exception as e:
        print(f"Failed to download image for item {item_id}: {e}")

cursor.close()
conn.close()
print("Image download completed!")
```

**2. 실행**
```bash
cd backend\capstone-backend\data
python download_images.py
```

---

## 🔧 고급 설정

### 대량 데이터 수집 (1000개 이상)

```bash
# 최근 60일, 대량 수집 (시간 소요: 5-10분)
python lost112_collect_and_sync_fast.py --days 60 --rows 100 --max-pages 200 --concurrency 3
```

**주의**: 
- API 요청 제한에 주의
- 너무 많은 페이지를 요청하면 시간 초과 가능
- `--concurrency` 값을 높이면 빠르지만 API 서버에 부담

### 스케줄링 (자동 수집)

**Windows 작업 스케줄러** 또는 **cron**으로 자동화:

```bash
# 매일 새벽 2시에 실행
# 작업 스케줄러에 다음 명령 등록:
python C:\...\lost112_collect_and_sync_fast.py --days 1 --rows 100 --max-pages 10
```

---

## 📊 데이터 확인

### 1. 총 데이터 개수

```sql
SELECT 
    datasource,
    COUNT(*) as count,
    MIN(found_date) as earliest,
    MAX(found_date) as latest
FROM lost_item
GROUP BY datasource;
```

### 2. 카테고리별 통계

```sql
SELECT category, COUNT(*) as count
FROM lost_item
WHERE datasource = 'LOST112'
GROUP BY category
ORDER BY count DESC
LIMIT 10;
```

### 3. 최근 수집된 데이터

```sql
SELECT id, title, category, location, found_date, image_path
FROM lost_item
WHERE datasource = 'LOST112'
ORDER BY created_at DESC
LIMIT 20;
```

### 4. 이미지가 있는 항목

```sql
SELECT COUNT(*) as count_with_images
FROM lost_item
WHERE image_path IS NOT NULL AND image_path != '';
```

---

## 🎯 실행 순서 (한 번에)

### 1. .env 파일 생성
```bash
cd backend\capstone-backend\data
notepad .env
```

내용:
```env
LOST112_SERVICE_KEY=your_api_key_here
DB_HOST=localhost
DB_PORT=3306
DB_NAME=capstonedb
DB_USER=hogeonhee
DB_PASSWORD=0316
```

### 2. Python 패키지 설치
```bash
pip install pandas requests python-dotenv mysql-connector-python
```

### 3. 데이터 수집
```bash
python lost112_collect_and_sync_fast.py --days 7 --rows 100 --max-pages 20
```

### 4. 백엔드 실행
```bash
cd ..\..
.\mvnw.cmd spring-boot:run
```

### 5. 브라우저 확인
```
http://localhost:5173
```

✅ 실제 LOST112 데이터가 표시됩니다!

---

## 🐛 문제 해결

### Python 스크립트 오류

**"ModuleNotFoundError: No module named 'pandas'"**
```bash
pip install pandas requests python-dotenv mysql-connector-python
```

**"Access denied for user"**
- `.env` 파일의 DB 정보 확인
- MySQL 사용자 권한 확인

### API 키 오류

**"Invalid service key"**
- LOST112 API 키가 올바른지 확인
- 키에 공백이나 특수문자가 없는지 확인

### 데이터가 표시되지 않음

```sql
-- 데이터 확인
SELECT COUNT(*) FROM lost_item;

-- 0이면 스크립트 재실행
```

---

## 📸 이미지 표시

프론트엔드에서 이미지를 표시하려면 `ItemCard.jsx`에서:

```jsx
{item.imagePath && (
  <img 
    src={item.imagePath} 
    alt={item.title}
    className="w-full h-48 object-cover rounded-t-lg"
    onError={(e) => e.target.style.display = 'none'}
  />
)}
```

---

## 🎉 성공!

- ✅ 실제 LOST112 데이터 수집
- ✅ 이미지 URL 포함
- ✅ 자동 중복 제거
- ✅ 카테고리별 검색 가능
- ✅ 지역별 검색 가능

**다음 단계**:
1. 서울시 지하철 분실물 추가
2. 이미지 다운로드 자동화
3. 정기적인 데이터 업데이트 스케줄링

