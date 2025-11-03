# 분실물 검색 기능 사용 가이드

## 🎯 기능 개요

지역과 분실물 카테고리를 선택하여 분실물을 검색할 수 있는 기능이 추가되었습니다.

## ✨ 주요 기능

### 1. 키워드 검색
- 제목, 설명, 카테고리, 색상, 위치 등 모든 필드에서 검색
- 실시간 검색 결과 표시

### 2. 카테고리 선택
다음 카테고리 중에서 선택할 수 있습니다:
- 전체
- 지갑
- 가방
- 핸드폰
- 노트북
- 서류
- 귀중품
- 의류
- 우산
- 도서
- 기타

### 3. 지역 선택
서울시 모든 구를 선택할 수 있습니다:
- 강남구, 강동구, 강북구, 강서구, 관악구, 광진구, 구로구, 금천구
- 노원구, 도봉구, 동대문구, 동작구, 마포구, 서대문구, 서초구, 성동구
- 성북구, 송파구, 양천구, 영등포구, 용산구, 은평구, 종로구, 중구, 중랑구

### 4. 복합 검색
- 키워드 + 카테고리
- 키워드 + 지역
- 카테고리 + 지역
- 키워드 + 카테고리 + 지역

모든 조건을 조합하여 검색할 수 있습니다.

## 📝 구현 파일

### Frontend
1. **`src/pages/Search.jsx`**
   - 검색 페이지 메인 컴포넌트
   - 검색 UI 및 결과 표시
   - 페이지네이션 기능

2. **`src/pages/Home.jsx`**
   - 메인 페이지에서 최근 분실물 표시
   - 검색 바에서 Search 페이지로 이동

3. **`src/components/ui/ItemCard.jsx`**
   - 분실물 카드 컴포넌트
   - 상태, 카테고리, 위치 등 정보 표시

4. **`src/lib/api.js`**
   - API 호출 함수 모음
   - 검색 관련 엔드포인트:
     - `list()` - 전체 목록
     - `searchByKeyword()` - 키워드 검색
     - `searchByRegion()` - 지역별 검색
     - `searchByCategory()` - 카테고리별 검색
     - `advancedSearch()` - 고급 검색 (복합 조건)
     - `searchFullText()` - 전체 텍스트 검색
     - `getRecent()` - 최근 분실물

### Backend API Endpoints
백엔드에서 제공하는 검색 관련 API:

1. **`GET /api/lost-items`** - 전체 목록 (페이징)
2. **`GET /api/lost-items/search?keyword={keyword}`** - 키워드 검색
3. **`GET /api/lost-items/search/region?region={region}`** - 지역별 검색
4. **`GET /api/lost-items/category/{category}`** - 카테고리별 검색
5. **`GET /api/lost-items/search/advanced`** - 고급 검색
   - 파라미터: `keyword`, `category`, `status`, `fromDate`, `toDate`
6. **`GET /api/lost-items/search/fulltext?text={text}`** - 전체 텍스트 검색
7. **`GET /api/lost-items/recent`** - 최근 등록 분실물 10개

## 🚀 실행 방법

### 1. 환경 변수 설정
프론트엔드 프로젝트 루트에 `.env` 파일 생성:
```bash
VITE_API_BASE_URL=http://localhost:8080
```

### 2. 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

### 3. 백엔드 실행
```bash
cd backend/capstone-backend
./mvnw spring-boot:run
```
또는
```bash
mvn spring-boot:run
```

## 💡 사용 방법

### 1. 홈 페이지에서 검색
1. 메인 페이지 상단의 검색 바에 키워드 입력
2. 검색 버튼 클릭 또는 Enter 키 입력
3. 자동으로 검색 페이지로 이동

### 2. 검색 페이지에서 상세 검색
1. `/search` 페이지 이동
2. 키워드 입력 (선택사항)
3. 카테고리 선택 (선택사항)
4. 지역 선택 (선택사항)
5. "검색" 버튼 클릭
6. 검색 결과 확인

### 3. 페이지네이션
- 검색 결과가 많을 경우 하단에 페이지네이션 표시
- 페이지당 12개 항목 표시
- "이전", "다음" 버튼으로 이동
- 페이지 번호 직접 클릭 가능

## 🎨 UI/UX 특징

### 검색 폼
- 깔끔한 카드 형태의 검색 폼
- 카테고리는 버튼 형태로 선택
- 지역은 드롭다운으로 선택
- 선택된 항목은 파란색으로 강조

### 분실물 카드
- 상태 배지 (습득, 수령완료, 만료 등)
- 카테고리 표시
- 습득장소, 보관장소 표시
- 습득일, 색상 정보
- 데이터 출처 표시 (사용자 등록, 경찰청, 서울시)

### 로딩 상태
- 검색 중 스피너 애니메이션 표시
- 부드러운 전환 효과

### 에러 처리
- 에러 발생 시 사용자 친화적인 메시지 표시
- 재시도 가능

## 🔧 커스터마이징

### 카테고리 추가/수정
`frontend/src/pages/Search.jsx` 파일의 `CATEGORIES` 배열 수정:
```javascript
const CATEGORIES = [
  "전체",
  "지갑",
  // ... 새로운 카테고리 추가
];
```

### 지역 추가/수정
`frontend/src/pages/Search.jsx` 파일의 `REGIONS` 배열 수정:
```javascript
const REGIONS = [
  "전체",
  "강남구",
  // ... 새로운 지역 추가
];
```

### 페이지당 항목 수 변경
`frontend/src/pages/Search.jsx` 파일의 `performSearch` 함수에서 수정:
```javascript
const searchParams = {
  page,
  size: 12, // 이 값을 변경
  sort: "createdAt,desc"
};
```

## 📊 API 응답 형식

### 검색 결과 응답
```json
{
  "code": 200,
  "message": "성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "지갑 습득",
        "description": "검은색 가죽 지갑",
        "category": "지갑",
        "location": "강남역 2번 출구",
        "foundDate": "2025-01-15",
        "status": "FOUND",
        "color": "검은색",
        "storageLocation": "강남구청 분실물센터",
        "dataSource": "USER",
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-01-15T10:30:00"
      }
    ],
    "totalPages": 5,
    "totalElements": 50,
    "size": 12,
    "number": 0
  }
}
```

## 🐛 문제 해결

### API 호출 실패
1. 백엔드 서버가 실행 중인지 확인
2. `.env` 파일의 `VITE_API_BASE_URL` 확인
3. 네트워크 콘솔에서 요청 URL 확인
4. CORS 설정 확인

### 검색 결과가 없음
1. 데이터베이스에 데이터가 있는지 확인
2. 검색 조건이 너무 제한적이지 않은지 확인
3. 백엔드 로그 확인

### 페이지네이션 오류
1. API 응답에 `totalPages` 필드가 있는지 확인
2. 페이지 번호가 0부터 시작하는지 확인

## 📚 참고 사항

- React Router의 `useSearchParams`를 사용하여 URL 쿼리 파라미터 처리
- Axios를 사용한 HTTP 요청
- Tailwind CSS를 사용한 스타일링
- 반응형 디자인 지원 (모바일, 태블릿, 데스크톱)

