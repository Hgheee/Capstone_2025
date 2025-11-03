# 빠른 시작 가이드 - 실제 데이터로!

## 🎯 목표

샘플 데이터 대신 **실제 LOST112 분실물 데이터**를 5분 안에 수집하고 표시합니다!

---

## ⚡ 5분 완성 (Windows)

### 1️⃣ API 키 설정 (1분)

`backend\capstone-backend\data\.env` 파일 생성:

```bash
cd backend\capstone-backend\data
notepad .env
```

파일 내용 (API 키만 수정):

```env
LOST112_SERVICE_KEY=여기에_실제_API키_입력
DB_HOST=localhost
DB_PORT=3306
DB_NAME=capstonedb
DB_USER=hogeonhee
DB_PASSWORD=0316
```

저장 후 닫기.

### 2️⃣ 자동 수집 실행 (2분)

프로젝트 루트에서:

```bash
collect_data.bat
```

또는 수동으로:

```bash
cd backend\capstone-backend\data
python lost112_collect_and_sync_fast.py --days 7 --rows 100 --max-pages 20
```

**진행 상황**:

```
[INFO] Collecting pages 1-20...
[INFO] Page 1/20 completed (100 items)
[INFO] Page 2/20 completed (100 items)
...
[INFO] Total collected: 1,247 items
[INFO] Inserting into database...
[INFO] Database sync completed!
```

### 3️⃣ 백엔드 실행 (1분)

```bash
cd backend\capstone-backend
mvnw.cmd spring-boot:run
```

### 4️⃣ 브라우저 확인 (1분)

```
http://localhost:5173
```

✅ **실제 LOST112 데이터가 표시됩니다!**

---

## 🔍 확인

### MySQL에서 확인

```bash
mysql -u hogeonhee -p capstonedb
```

```sql
-- 총 데이터 개수
SELECT COUNT(*) FROM lost_item WHERE datasource = 'LOST112';

-- 최근 10개
SELECT id, title, category, location FROM lost_item
WHERE datasource = 'LOST112'
ORDER BY created_at DESC
LIMIT 10;

-- 카테고리별 통계
SELECT category, COUNT(*) as count
FROM lost_item
WHERE datasource = 'LOST112'
GROUP BY category
ORDER BY count DESC;
```

---

## 📸 이미지 포함된 데이터

LOST112 데이터는 자동으로 이미지 URL이 포함됩니다!

```sql
SELECT COUNT(*) FROM lost_item
WHERE datasource = 'LOST112'
AND image_path IS NOT NULL;
```

프론트엔드 `ItemCard.jsx`가 자동으로 이미지를 표시합니다.

---

## 🎛️ 수집 옵션

### 소량 (빠름)

```bash
python lost112_collect_and_sync_fast.py --days 1 --rows 100 --max-pages 10
# 약 1분, 100-500개
```

### 중간 (추천)

```bash
python lost112_collect_and_sync_fast.py --days 7 --rows 100 --max-pages 20
# 약 2-3분, 500-2000개
```

### 대량 (느림)

```bash
python lost112_collect_and_sync_fast.py --days 30 --rows 100 --max-pages 100
# 약 5-10분, 2000-10000개
```

---

## 🔧 문제 해결

### "ModuleNotFoundError"

```bash
pip install pandas requests python-dotenv mysql-connector-python
```

### "Access denied"

- `.env` 파일의 DB 설정 확인
- MySQL 사용자 권한 확인 (이전 가이드 참조)

### "Invalid service key"

- LOST112 API 키가 올바른지 확인
- https://www.lost112.go.kr/main.do 에서 재발급

---

## 📊 예상 결과

### 홈 페이지

```
최근 등록된 분실물

┌──────────────────────┐
│ [습득] 지갑          │
│ 서울특별시 강남구... │
│ 습득: 2025-01-15     │
│ [이미지]             │
└──────────────────────┘
```

### 검색 페이지

```
총 1,247개의 분실물 (LOST112)

[전체] [지갑] [가방] [핸드폰] ...

[서울특별시▼] [강남구▼]

실제 데이터 표시!
```

---

## 🎉 완료!

이제 실제 LOST112 데이터로:

- ✅ 지역별 검색
- ✅ 카테고리별 검색
- ✅ 이미지 포함
- ✅ 실시간 업데이트 가능

**다음 단계**:

- 정기적 업데이트 스케줄링
- 서울시 지하철 데이터 추가
- 이미지 다운로드 자동화

상세 가이드: `DATA_COLLECTION_GUIDE.md` 참조
