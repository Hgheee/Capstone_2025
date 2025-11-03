# 빠른 문제 해결 가이드

## 🐛 현재 문제

1. ❌ "최근 등록된 분실물이 없습니다"
2. ❌ 검색 페이지를 찾을 수 없음 (404)

## ✅ 해결 방법 (순서대로 진행)

### 1️⃣ 검색 페이지 라우트 추가 ✅ (완료)

**수정된 파일**: `frontend/src/App.jsx`

```javascript
import Search from "./pages/Search.jsx";  // ✅ 추가됨
<Route path="/search" element={<Search />} />  // ✅ 추가됨
```

### 2️⃣ 데이터베이스에 데이터 추가 (필수!)

#### 방법 1: MySQL에서 직접 실행 (추천)

```bash
# MySQL 접속
mysql -u hogeonhee -p capstonedb
# 비밀번호: 0316
```

그리고 다음 SQL을 실행하세요:

```sql
-- 현재 데이터 확인
SELECT COUNT(*) FROM lost_item;

-- 데이터가 0이면 샘플 데이터 삽입
INSERT INTO lost_item (title, description, category, location, found_date, status, color, datasource, user_id) VALUES
('검은색 지갑', '신한은행 카드가 들어있는 가죽 지갑', '지갑', '강남역 2번 출구', '2025-01-15', 'FOUND', '검은색', 'USER', 1),
('빨간색 우산', '접이식 자동 우산', '우산', '서초구 서초대로', '2025-01-14', 'FOUND', '빨간색', 'USER', 1),
('아이폰 15', '흰색 실리콘 케이스 장착', '핸드폰', '강남구 테헤란로', '2025-01-13', 'FOUND', '흰색', 'USER', 1),
('노트북 가방', '검은색 백팩 형태의 노트북 가방', '가방', '송파구 올림픽로', '2025-01-12', 'FOUND', '검은색', 'USER', 1),
('에어팟', '에어팟 프로 2세대 케이스', '귀중품', '강동구 천호역', '2025-01-11', 'FOUND', '흰색', 'USER', 1),
('삼성 갤럭시', '검은색 케이스 장착', '핸드폰', '송파구', '2025-01-10', 'FOUND', '검은색', 'USER', 1),
('에어팟 맥스', '실버 색상', '귀중품', '강남구 역삼역', '2025-01-09', 'FOUND', '실버', 'USER', 1),
('신분증', '주민등록증', '서류', '마포구 홍대입구역', '2025-01-08', 'FOUND', NULL, 'USER', 1),
('맥북 프로', '16인치 M3 Pro', '노트북', '강남구 선릉역', '2025-01-07', 'FOUND', '스페이스 그레이', 'USER', 1),
('패딩 점퍼', '노스페이스 검은색', '의류', '서초구 강남역', '2025-01-06', 'FOUND', '검은색', 'USER', 1);

-- 데이터 확인
SELECT id, title, category, location FROM lost_item;
```

#### 방법 2: 스크립트 파일 사용

```bash
# 프로젝트 루트에서
mysql -u hogeonhee -p capstonedb < check_and_fix_database.sql
```

### 3️⃣ 백엔드 재시작

```bash
cd backend\capstone-backend

# Windows
mvnw.cmd spring-boot:run
```

**확인사항**: 로그에서 다음 메시지 확인
```
Started CapstoneBackendApplication
```

### 4️⃣ 프론트엔드 재시작 (또는 새로고침)

```bash
# 프론트엔드가 실행 중이면 브라우저만 새로고침
# 또는 재시작
cd frontend
npm run dev
```

### 5️⃣ 브라우저 테스트

```
http://localhost:5173
```

✅ **확인사항**:
- [ ] 홈 페이지에 최근 분실물 표시
- [ ] "전체보기" 버튼 클릭 → 검색 페이지로 이동
- [ ] 검색 페이지에서 분실물 목록 표시

---

## 🧪 빠른 테스트

### 1. 데이터베이스 직접 확인

```bash
mysql -u hogeonhee -p capstonedb -e "SELECT COUNT(*) FROM lost_item;"
```

**예상 결과**: `10` 이상

### 2. API 직접 테스트

```bash
curl http://localhost:8080/api/lost-items/recent
```

**예상 결과**: JSON 데이터 (분실물 목록)

### 3. 브라우저 개발자 도구

F12 → Console 탭:
- ❌ 에러 메시지가 있으면 복사해서 알려주세요
- ✅ 에러가 없으면 성공!

---

## 🔍 문제 지속 시 체크리스트

### 데이터베이스
```sql
-- MySQL에서 실행
USE capstonedb;

-- 1. 테이블 존재 확인
SHOW TABLES;
-- 결과: lost_item 테이블이 있어야 함

-- 2. 데이터 개수
SELECT COUNT(*) FROM lost_item;
-- 결과: 0보다 커야 함

-- 3. 사용자 확인
SELECT * FROM users;
-- 결과: 최소 1개 이상
```

### 백엔드
```bash
# 8080 포트 확인
netstat -an | findstr 8080
```

### 프론트엔드
```bash
# 5173 포트 확인
netstat -an | findstr 5173

# .env 파일 확인
type frontend\.env
```

---

## 🎯 한 번에 해결하기

### Windows에서:

```bash
# 1. 데이터베이스에 데이터 추가
mysql -u hogeonhee -p capstonedb < check_and_fix_database.sql

# 2. 백엔드 재시작 (새 터미널)
cd backend\capstone-backend
mvnw.cmd spring-boot:run

# 3. 프론트엔드 새로고침
# 브라우저에서 Ctrl+F5
```

---

## 📋 예상 결과

### 홈 페이지
```
최근 등록된 분실물

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ [습득] 지갑  │ │ [습득] 우산  │ │ [습득] 핸드폰│
│ 검은색 지갑  │ │ 빨간색 우산  │ │ 아이폰 15    │
│ 강남역 2번...│ │ 서초구 서... │ │ 강남구 테... │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 검색 페이지
```
분실물 검색
총 10개의 분실물

[전체] [지갑] [가방] [핸드폰] ...

┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ 분실물 카드 1│ │ 분실물 카드 2│ │ 분실물 카드 3│
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## ❓ 여전히 문제가 있다면

### 1. 에러 메시지 복사
브라우저 F12 → Console 탭의 에러 메시지를 복사해주세요.

### 2. 백엔드 로그 확인
백엔드 콘솔에서 에러 메시지를 복사해주세요.

### 3. 데이터베이스 상태
```sql
-- 이 명령어 실행 결과를 알려주세요
USE capstonedb;
SHOW TABLES;
SELECT COUNT(*) FROM lost_item;
SELECT * FROM users LIMIT 1;
```

---

## ✅ 해결 완료 체크리스트

- [ ] 데이터베이스에 10개 이상의 분실물 데이터
- [ ] 백엔드 8080 포트에서 실행 중
- [ ] 프론트엔드 5173 포트에서 실행 중
- [ ] 홈 페이지에 최근 분실물 표시
- [ ] 검색 페이지 정상 작동
- [ ] 카테고리/지역 검색 작동

모두 체크되면 완료! 🎉

