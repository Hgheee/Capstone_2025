# 통합 테스트 및 실행 가이드

## 🎯 완료된 리팩토링 내용

### ✅ 백엔드 수정사항
1. **데이터베이스 스키마 완전히 재작성**
   - `schema.sql`: LostItem 엔티티와 100% 일치
   - 모든 필드, 인덱스, 외래키 포함
   - 샘플 데이터 5개 포함

2. **application.yml 최적화**
   - 데이터베이스: `capstonedb`
   - JPA 설정: `ddl-auto: update` (개발 환경)
   - SQL 로그 활성화
   - 커넥션 풀 설정

### ✅ 프론트엔드 수정사항
1. **API 응답 처리 개선**
   - 백엔드 `ApiResponse` 구조 정확히 매핑
   - `{ success, data, error }` 구조 처리
   - 에러 핸들링 강화

2. **api.js 리팩토링**
   - axios 인터셉터 추가
   - 에러 처리 개선
   - 토큰 관리 자동화
   - 모든 API 엔드포인트 정리

3. **Search.jsx 개선**
   - Page 객체와 List 객체 모두 처리
   - 에러 메시지 상세화
   - 응답 구조 검증

4. **Home.jsx 개선**
   - API 응답 검증
   - 에러 처리

## 🚀 실행 순서

### 1️⃣ 데이터베이스 설정 (5분)

```bash
# MySQL 접속
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 사용자 생성 및 권한
CREATE USER IF NOT EXISTS 'hogeonhee'@'localhost' IDENTIFIED BY '0316';
GRANT ALL PRIVILEGES ON capstonedb.* TO 'hogeonhee'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

상세 내용: `DATABASE_SETUP_GUIDE.md` 참조

### 2️⃣ 백엔드 실행 (3분)

```bash
cd backend/capstone-backend

# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

**확인사항**:
- ✅ 서버가 8080 포트에서 실행
- ✅ 데이터베이스 연결 성공
- ✅ 스키마 자동 생성 완료
- ✅ 샘플 데이터 삽입 완료

로그에서 확인:
```
Started CapstoneBackendApplication in X.XXX seconds
```

### 3️⃣ 프론트엔드 설정 (2분)

```bash
cd frontend

# 환경 변수 파일 생성
echo VITE_API_BASE_URL=http://localhost:8080 > .env

# 의존성 설치 (최초 1회만)
npm install

# 개발 서버 실행
npm run dev
```

**확인사항**:
- ✅ 서버가 5173 포트에서 실행
- ✅ `.env` 파일 생성 확인
- ✅ 브라우저에서 http://localhost:5173 접속

## 🧪 테스트 시나리오

### 1. 홈 페이지 테스트

**URL**: http://localhost:5173/

**확인사항**:
- [ ] 최근 등록된 분실물 6개 표시
- [ ] 각 카드에 제목, 설명, 카테고리 표시
- [ ] 검색 바 정상 작동
- [ ] 검색어 입력 후 Enter → 검색 페이지로 이동

**예상 결과**:
```
최근 등록된 분실물
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ [습득] 지갑  │ │ [습득] 우산  │ │ [습득] 핸드폰│
│ 검은색 지갑  │ │ 빨간색 우산  │ │ 아이폰 15    │
│ 강남역 2번... │ │ 서초구 서... │ │ 강남구 테... │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 2. 검색 페이지 - 전체 목록

**URL**: http://localhost:5173/search

**테스트**:
1. 아무 조건도 선택하지 않음
2. 페이지 로드 확인

**확인사항**:
- [ ] 모든 분실물 표시 (최소 5개)
- [ ] 페이지네이션 표시
- [ ] 로딩 스피너 표시 후 사라짐

### 3. 검색 페이지 - 카테고리 검색

**테스트**:
1. 카테고리 "지갑" 클릭
2. 결과 확인

**확인사항**:
- [ ] "지갑" 버튼이 파란색으로 강조
- [ ] 지갑 카테고리의 분실물만 표시
- [ ] 결과 개수 표시

**예상 결과**:
```
총 1개의 분실물

┌──────────────┐
│ [습득] 지갑  │
│ 검은색 지갑  │
│ 강남역 2번...│
└──────────────┘
```

### 4. 검색 페이지 - 지역 검색

**테스트**:
1. 지역 드롭다운에서 "강남구" 선택
2. 결과 확인

**확인사항**:
- [ ] 강남구 관련 분실물만 표시
- [ ] 습득장소 또는 보관장소에 "강남구" 포함

### 5. 검색 페이지 - 복합 검색

**테스트**:
1. 키워드: "지갑" 입력
2. 카테고리: "지갑" 선택
3. 지역: "강남구" 선택
4. 검색 버튼 클릭

**확인사항**:
- [ ] 모든 조건을 만족하는 결과만 표시
- [ ] 조건에 맞는 항목이 없으면 "검색 결과가 없습니다" 메시지

### 6. 검색 페이지 - 키워드 검색

**테스트**:
1. 키워드: "우산" 입력
2. 검색 버튼 클릭

**확인사항**:
- [ ] 제목, 설명, 카테고리 등에 "우산" 포함된 항목 표시

### 7. 페이지네이션 테스트

**전제조건**: 분실물이 13개 이상 있어야 함

**테스트**:
1. 전체 목록 조회
2. "다음" 버튼 클릭
3. "이전" 버튼 클릭
4. 페이지 번호 직접 클릭

**확인사항**:
- [ ] 페이지당 12개씩 표시
- [ ] 페이지 번호 버튼 정상 작동
- [ ] 첫 페이지에서 "이전" 버튼 비활성화
- [ ] 마지막 페이지에서 "다음" 버튼 비활성화

## 📊 데이터 확인

### MySQL에서 직접 확인

```bash
mysql -u hogeonhee -p capstonedb
# 비밀번호: 0316
```

```sql
-- 전체 데이터 확인
SELECT id, title, category, location, status FROM lost_item;

-- 카테고리별 개수
SELECT category, COUNT(*) FROM lost_item GROUP BY category;

-- 지역별 개수
SELECT location, COUNT(*) FROM lost_item GROUP BY location;
```

### 백엔드 API 직접 테스트

#### 1. 전체 목록 조회
```bash
curl http://localhost:8080/api/lost-items
```

#### 2. 카테고리 검색
```bash
curl http://localhost:8080/api/lost-items/category/지갑
```

#### 3. 지역 검색
```bash
curl "http://localhost:8080/api/lost-items/search/region?region=강남"
```

#### 4. 고급 검색
```bash
curl "http://localhost:8080/api/lost-items/search/advanced?keyword=지갑&category=지갑"
```

## 🐛 문제 해결

### 1. "검색 결과가 없습니다"

**원인**: 데이터베이스에 데이터가 없음

**해결**:
```sql
-- MySQL에서 데이터 확인
SELECT * FROM lost_item;

-- 데이터가 없으면 schema.sql 재실행
source C:/path/to/backend/capstone-backend/src/main/resources/schema.sql;
```

또는 백엔드 재시작:
```bash
cd backend/capstone-backend
mvnw.cmd spring-boot:run
```

### 2. API 호출 실패 (Network Error)

**원인**: 백엔드 서버가 실행되지 않음

**확인**:
```bash
# 8080 포트 확인
netstat -an | findstr 8080

# 백엔드 로그 확인
# "Started CapstoneBackendApplication" 메시지 확인
```

**해결**: 백엔드 재실행

### 3. CORS 오류

**오류**: `Access to XMLHttpRequest has been blocked by CORS policy`

**확인**: `application-dev.yml`에서 CORS 설정 확인
```yaml
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
```

### 4. 데이터베이스 연결 실패

**오류**: `Communications link failure`

**해결**:
```bash
# MySQL 서비스 시작
net start MySQL80

# MySQL 상태 확인
mysql -u hogeonhee -p
```

### 5. 한글 깨짐

**해결**:
```sql
-- 데이터베이스 문자셋 변경
ALTER DATABASE capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## ✨ 추가 데이터 삽입

더 많은 테스트 데이터가 필요하면:

```sql
USE capstonedb;

INSERT INTO lost_item (title, description, category, location, found_date, status, color, datasource, user_id) VALUES
('삼성 갤럭시', '검은색 케이스 장착', '핸드폰', '서울시 송파구', '2025-01-10', 'FOUND', '검은색', 'USER', 1),
('에어팟 맥스', '실버 색상', '귀중품', '강남구 역삼역', '2025-01-09', 'FOUND', '실버', 'USER', 1),
('신분증', '주민등록증', '서류', '마포구 홍대입구역', '2025-01-08', 'FOUND', NULL, 'USER', 1),
('맥북 프로', '16인치 M3 Pro', '노트북', '강남구 선릉역', '2025-01-07', 'FOUND', '스페이스 그레이', 'USER', 1),
('패딩 점퍼', '노스페이스 검은색', '의류', '서초구 강남역', '2025-01-06', 'FOUND', '검은색', 'USER', 1);
```

## 📈 성능 확인

### 응답 시간 측정

```bash
# 전체 목록 조회 (페이징)
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/api/lost-items

# 검색 (키워드)
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/lost-items/search?keyword=지갑"
```

`curl-format.txt`:
```
time_total: %{time_total}s\n
```

**목표**: 1초 이내

### 브라우저 개발자 도구

1. F12 → Network 탭
2. 페이지 새로고침
3. API 호출 확인
   - Status: 200 OK
   - Time: < 1s
   - Response: JSON 형식

## ✅ 최종 체크리스트

### 백엔드
- [ ] MySQL 실행 중
- [ ] capstonedb 데이터베이스 존재
- [ ] lost_item 테이블 존재
- [ ] 샘플 데이터 최소 5개
- [ ] 백엔드 서버 8080 포트에서 실행
- [ ] API 응답 정상 (curl 테스트)

### 프론트엔드
- [ ] .env 파일 존재
- [ ] npm install 완료
- [ ] 프론트엔드 서버 5173 포트에서 실행
- [ ] 홈 페이지 로딩 성공
- [ ] 검색 페이지 로딩 성공

### 기능 테스트
- [ ] 홈 페이지 최근 분실물 표시
- [ ] 검색 바 정상 작동
- [ ] 카테고리 검색 정상
- [ ] 지역 검색 정상
- [ ] 복합 검색 정상
- [ ] 페이지네이션 정상
- [ ] 분실물 카드 정보 정확히 표시

## 🎉 성공!

모든 테스트가 통과하면 성공입니다! 

이제 실제 데이터(LOST112, 서울시 분실물 등)를 수집하여 시스템을 운영할 수 있습니다.

---

## 📞 지원

문제가 있으면:
1. 이 문서의 "문제 해결" 섹션 확인
2. `DATABASE_SETUP_GUIDE.md` 참조
3. `SEARCH_FEATURE_README.md` 참조
4. 백엔드/프론트엔드 로그 확인

