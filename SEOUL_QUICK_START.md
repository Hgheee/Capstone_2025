# 서울시 분실물 데이터 수집 가이드

## 🚇 서울시 데이터로 전환!

LOST112 대신 **서울시 열린데이터광장**의 분실물 데이터를 수집합니다.

---

## ⚡ 3분 완성

### 1️⃣ API 키 발급 (처음 1회만)

1. https://data.seoul.go.kr 접속
2. 회원가입 / 로그인
3. **마이페이지** → **인증키 신청**
4. API 키 복사

### 2️⃣ .env 파일 수정

```powershell
cd backend\capstone-backend\data
notepad .env
```

파일 내용:
```env
# 서울시 API 키 추가!
SEOUL_API_KEY=여기에_서울시_API키_붙여넣기

# MySQL (기존)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=capstonedb
DB_USER=hogeonhee
DB_PASSWORD=0316
```

### 3️⃣ 데이터 수집 실행

```powershell
python collect_seoul_data.py
```

**진행 상황**:
```
[1] 📡 요청: 1-100번 항목
    ✅ 100개 수신
    💾 95개 저장

[2] 📡 요청: 101-200번 항목
    ✅ 100개 수신
    💾 98개 저장
...

✅ 수집 완료!
   총 수집: 500개
   신규 저장: 485개
   중복 건너뜀: 15개
```

### 4️⃣ 백엔드 실행

```powershell
cd ..\..
mvnw.cmd spring-boot:run
```

### 5️⃣ 확인

```
http://localhost:5173
```

✅ **서울시 지하철/버스 분실물 데이터가 표시됩니다!**

---

## 🔍 데이터 확인

```sql
mysql -u hogeonhee -p capstonedb

-- 서울시 데이터 개수
SELECT COUNT(*) FROM lost_item WHERE datasource = 'SEOUL_LOST';

-- 최근 10개
SELECT id, title, category, location, storage_location 
FROM lost_item 
WHERE datasource = 'SEOUL_LOST' 
ORDER BY created_at DESC 
LIMIT 10;

-- 카테고리별 통계
SELECT category, COUNT(*) as count 
FROM lost_item 
WHERE datasource = 'SEOUL_LOST' 
GROUP BY category 
ORDER BY count DESC;
```

---

## 📊 예상 데이터

### 데이터 출처
- 서울교통공사 (지하철 1-9호선)
- 서울시 버스
- 서울시 공공시설

### 예상 수집량
- 초기 실행: 500-1,000개
- 재실행 시: 신규 데이터만 추가

### 데이터 형태
```
제목: 검은색 지갑
카테고리: 지갑
습득장소: 2호선 강남역
보관장소: 강남역 역무실
상태: 보관중
```

---

## 🎯 백엔드 API로 수집

Python 스크립트 대신 백엔드 API 사용 가능:

### 1. 백엔드 실행
```powershell
cd backend\capstone-backend
mvnw.cmd spring-boot:run
```

### 2. Swagger 접속
```
http://localhost:8080/swagger-ui/index.html
```

### 3. Admin Controller → POST /api/admin/seoul/import

**Execute** 클릭!

응답:
```json
{
  "success": true,
  "message": "서울시 분실물 100건 수집 완료"
}
```

---

## 🔧 문제 해결

### "❌ SEOUL_API_KEY가 설정되지 않았습니다"

→ `.env` 파일에 `SEOUL_API_KEY=실제키` 추가

### "❌ DB 연결 실패"

→ MySQL 실행 확인
→ `.env` 파일의 DB 정보 확인

### "⚠️ API 오류: Invalid key"

→ API 키가 잘못됨
→ https://data.seoul.go.kr 에서 키 재발급

### "더 이상 데이터 없음"

→ 정상! 모든 데이터 수집 완료
→ 나중에 다시 실행하면 신규 데이터만 추가됨

---

## 💡 추가 옵션

### 더 많은 데이터 수집

`collect_seoul_data.py` 수정:
```python
max_items = 2000  # 1000 → 2000으로 변경
```

### 정기적 업데이트

매일 실행:
```powershell
python collect_seoul_data.py
```

중복은 자동으로 건너뛰므로 안전합니다!

---

## ✅ 완료!

이제 실제 서울시 분실물 데이터로:
- ✅ 지역별 검색 (지하철 역별)
- ✅ 카테고리별 검색
- ✅ 실시간 데이터
- ✅ 보관 장소 정보

**API 키만 있으면 바로 작동합니다!** 🚇

