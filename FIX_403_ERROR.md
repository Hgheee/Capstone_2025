# 403 Forbidden 에러 해결 가이드

## 🐛 문제 상황

프론트엔드에서 API 호출 시 다음 에러 발생:
```
AxiosError: Request failed with status code 403
```

## 🔍 원인

**JwtAuthenticationFilter**의 `shouldNotFilter()` 메서드에 `/api/lost-items` 경로가 포함되지 않아서, 인증 필터가 실행되어 403 에러가 발생했습니다.

- `SecurityConfig`에서는 `permitAll()` 설정 ✅
- `JwtAuthenticationFilter`에서 필터 제외 설정 없음 ❌

## ✅ 해결 방법

### 1. 파일 수정 완료

**파일**: `backend/capstone-backend/src/main/java/com/lostfound/capstonebackend/config/JwtAuthenticationFilter.java`

**159번 줄에 추가**:
```java
path.startsWith("/api/lost-items") ||  // ✅ 분실물 조회 API 추가
```

### 2. 백엔드 재시작

```bash
# 기존 백엔드 종료 (Ctrl+C)

# 재시작
cd backend/capstone-backend
mvnw.cmd spring-boot:run
```

### 3. 확인

백엔드가 실행되면 브라우저를 새로고침하세요.

```
http://localhost:5173
```

✅ 홈 페이지에서 최근 분실물이 표시되면 성공!

## 🧪 테스트

### 1. 브라우저 콘솔 확인
F12 → Console 탭에서:
- ❌ 이전: `403 Forbidden` 에러
- ✅ 이후: 에러 없음, 데이터 정상 표시

### 2. API 직접 테스트
```bash
curl http://localhost:8080/api/lost-items/recent
```

**예상 결과**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "검은색 지갑",
      "category": "지갑",
      ...
    }
  ],
  "error": null
}
```

### 3. 네트워크 탭 확인
F12 → Network 탭:
- Status: **200 OK** ✅
- Response: JSON 데이터

## 📋 수정된 경로 목록

이제 다음 경로들은 인증 없이 접근 가능합니다:

```java
/                              // 홈
/api/health                    // 헬스체크
/api/auth/login                // 로그인
/api/auth/signup               // 회원가입
/api/lost-items/**             // 🆕 분실물 조회 API (모든 하위 경로)
/swagger-ui/**                 // Swagger UI
/v3/api-docs/**                // API 문서
/favicon.ico                   // 파비콘
/static/**                     // 정적 파일
/error                         // 에러 페이지
```

## 🔐 보안 참고사항

### 공개 API (인증 불필요)
- ✅ GET `/api/lost-items` - 전체 목록
- ✅ GET `/api/lost-items/recent` - 최근 분실물
- ✅ GET `/api/lost-items/search` - 검색
- ✅ GET `/api/lost-items/{id}` - 상세 조회

### 인증 필요 API
- 🔒 POST `/api/lost-items` - 분실물 등록
- 🔒 PUT `/api/lost-items/{id}` - 분실물 수정
- 🔒 DELETE `/api/lost-items/{id}` - 분실물 삭제
- 🔒 GET `/api/lost-items/my` - 내 분실물

## 🎯 핵심 개념

### Spring Security 필터 순서

```
1. JwtAuthenticationFilter.shouldNotFilter() 체크
   └─ true → 필터 건너뜀 (인증 불필요)
   └─ false → 필터 실행 → JWT 검증

2. SecurityFilterChain 체크
   └─ permitAll() → 인증 없어도 통과
   └─ authenticated() → 인증 필요
```

**중요**: `shouldNotFilter()`가 `false`를 반환하면 필터가 실행되므로, 공개 API는 `shouldNotFilter()`에 명시하는 것이 좋습니다.

## 🐛 추가 문제 해결

### 여전히 403 에러가 발생하면

#### 1. 백엔드 재시작 확인
```bash
# 완전히 종료 후 재시작
# Windows: Ctrl+C
# 재시작
mvnw.cmd spring-boot:run
```

#### 2. 로그 확인
백엔드 콘솔에서:
```
[JwtAuthenticationFilter] JWT 인증 필터에서 오류 발생
```
이런 로그가 보이면 필터가 여전히 실행 중

#### 3. 코드 확인
```java
// JwtAuthenticationFilter.java 159번 줄 확인
path.startsWith("/api/lost-items") ||  // 이 줄이 있는지 확인
```

#### 4. 빌드 재시도
```bash
cd backend/capstone-backend
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

#### 5. 브라우저 캐시 삭제
- Ctrl+Shift+Delete → 캐시 삭제
- 또는 시크릿 모드로 테스트

### CORS 에러가 발생하면

```bash
# application-dev.yml 확인
cors:
  allowed-origins: http://localhost:5173
```

## ✅ 해결 확인 체크리스트

- [ ] 백엔드 재시작 완료
- [ ] 프론트엔드 새로고침
- [ ] 홈 페이지에 최근 분실물 표시
- [ ] 브라우저 콘솔에 403 에러 없음
- [ ] 검색 페이지 정상 작동

모두 체크되면 완료! 🎉

## 📚 관련 문서

- `INTEGRATION_TEST_GUIDE.md` - 전체 테스트 가이드
- `DATABASE_SETUP_GUIDE.md` - 데이터베이스 설정
- `REFACTORING_SUMMARY.md` - 리팩토링 요약

