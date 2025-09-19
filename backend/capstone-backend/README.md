.# Capstone Backend (Spring Boot 3 + JWT + MySQL)

## 개요

- 분실물관리백엔드 (인증인가핵심
)- 스택: Spring Boot 3.5.x, Spring Security (JWT), Spring Data JPA, MySQL, HikariCP, springdoc-openapi
- 목표: 회원가입로그인내정보로그아웃을견고하게구현하고도메인(Lost Item)으로확장

## 실행방법

### 필수환경변수

- DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET_KEY, SERVER_PORT(옵션
)- 예시(Windows IntelliJ Run/Debug → Environment variables 한줄):
  DB_URL=jdbc:mysql://localhost:3306/capstone_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true;DB_USERNAME=capstone_user;DB_PASSWORD=1q2w3e4r;JWT_SECRET_KEY=this_is_a_demo_secret_key_32_bytes!!;SPRING_PROFILES_ACTIVE=dev;SERVER_PORT=8081

### IntelliJ 실행

- Run → Edit Configurations → Spring Boot → Environment variables 에위한줄입력→ Run

### Maven 실행대안
)- PowerShell:
  .\mvnw spring-boot:run -D"spring-boot.run.arguments=--logging.level.root=DEBUG --debug" -D"spring-boot.run.jvmArguments=-DSPRING_PROFILES_ACTIVE=dev -DDB_URL=... -DDB_USERNAME=... -DDB_PASSWORD=... -DJWT_SECRET_KEY=... -DSERVER_PORT=8081"

## 주요 API (예시 cURL)
### 회원가입
curl -X POST "http://localhost:8081/api/auth/signup" -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"password123!","name":"홍길동","phone":"010-1234-5678"}'

### 로그인토큰발급
)curl -X POST "http://localhost:8081/api/auth/login" -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"password123!"}'

### 내정보보호, 토큰필요
)curl -H "Authorization: Bearer <ACCESS_TOKEN>" "http://localhost:8081/api/auth/me"

### 로그아웃보호, 토큰블랙리스트
)curl -X POST -H "Authorization: Bearer <ACCESS_TOKEN>" "http://localhost:8081/api/auth/logout"

## 간단 ERD
users (id, email*, password, name, phone, timestamps)
roles (id, name)
user_roles (user_id, role_id)
blacklisted_token (id, token*, expires_at)
(*) unique

## 개발메모

- 프로파일: dev/test/prod
- JPA DDL: dev는 update 권장데이터유지), 초기실험은 create-drop
- Swagger UI: http://localhost:8081/swagger-ui/index.html
- 로깅: root/hibernate/binder 레벨조절로 SQL·바인딩추적가능

## 라이선스


# Capstone_2025 – Backend Readme (요약 문서)

## 1) 프로젝트 개요
- **목표**: 분실물 관리 서비스의 **백엔드 인증·인가 코어** 구축
- **핵심 플로우**: 회원가입(가입/중복검사) → 로그인(JWT 발급) → 내 정보 조회(보호 API) → 로그아웃(토큰 블랙리스트)
- **기술 스택**: Spring Boot 3.5.x, Spring Security (JWT), Spring Data JPA, MySQL, HikariCP, Jakarta Validation, springdoc-openapi(Swagger)

---

## 2) 구현 기능 (What)
### 인증/인가 API
- `POST /api/auth/signup`  
  - 새로운 사용자 등록(이메일 중복 검사)  
  - 비밀번호 **BCrypt 해시** 저장
- `POST /api/auth/login`  
  - 자격증명 검증 → **JWT 액세스 토큰** 발급 (예: 900초)
- `GET /api/auth/me` *(보호)*  
  - 헤더의 Bearer 토큰 검증 후 로그인 사용자 정보 반환
- `POST /api/auth/logout` *(보호)*  
  - 전달된 토큰을 **blacklisted_token** 테이블에 등록 → 이후 동일 토큰은 즉시 거부

### 데이터 모델
- `users`(유저 기본정보, 비밀번호 해시, 이메일 **unique**)
- `roles`, `user_roles`(권한 부여)
- `blacklisted_token`(로그아웃 처리된 JWT 저장, **token unique**)

---

## 3) 어떻게 사용하나 (How to Use)
### A. 로컬 실행 준비
1) **환경 변수**(IntelliJ → Run/Debug → *Environment variables* 한 줄)
```
DB_URL=jdbc:mysql://localhost:3306/capstone_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true;DB_USERNAME=<DB_USERNAME>;DB_PASSWORD=<DB_PASSWORD>;JWT_SECRET_KEY=<32+ chars secret>;SPRING_PROFILES_ACTIVE=dev;SERVER_PORT=8081
```

2) **DB 준비(SQL)** – 최초 1회
```sql
CREATE DATABASE IF NOT EXISTS capstone_db DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '<DB_USERNAME>'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON capstone_db.* TO '<DB_USERNAME>'@'localhost';
FLUSH PRIVILEGES;
```

3) **애플리케이션 실행**
- IntelliJ에서 `CapstoneBackendApplication` 실행
- Swagger: `http://localhost:8081/swagger-ui/index.html`

### B. API 호출 예시 (Swagger 또는 curl)
- **회원가입**
```bash
POST /api/auth/signup
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```
- **로그인(토큰 발급)**
```bash
POST /api/auth/login
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123!"
}
# 응답: { data.accessToken: "<JWT>" }
```
- **내 정보 (보호)**
```bash
GET /api/auth/me
Authorization: Bearer <JWT>
```
- **로그아웃 (보호)**
```bash
POST /api/auth/logout
Authorization: Bearer <JWT>
# 이후 같은 토큰으로 /me 호출 시 403 기대
```

---

## 4) 어떤 방법으로 구현했나 (How it works)
### 보안/인증 흐름
- **JWT 발급**: 로그인 성공 시 사용자 식별자(`sub`) 포함 토큰 생성(서명 키=`JWT_SECRET_KEY`)
- **요청 검증**: Security 필터가 `Authorization: Bearer …` 파싱 → 유효성/만료 확인 → `SecurityContext` 세팅
- **로그아웃**: 서버가 토큰 자체를 만료시킬 수 없으므로 **블랙리스트**에 저장 → 필터에서 매 요청 시 블랙리스트 조회해 차단
- **권한(roles)**: `user_roles` 매핑 기반; 필요 시 엔드포인트에 권한 제한 적용 가능

### 데이터/트랜잭션
- **JPA + HikariCP**로 영속성 관리
- 서비스 계층에서 **@Transactional**로 쓰기/읽기 경계 명확화(`readOnly=true` 활용)
- **Jakarta Validation**으로 DTO 유효성 검사(컨트롤러 진입 전 실패 처리)

### 예외/응답 일관화
- 전역 예외 핸들러로 `{ success, data, error }` 포맷 유지
- 에러 코드/메시지 통일

---

## 5) 이번 변경에서 무엇을 바꿨나 (Changes)
- **인증 코어 완성**: 회원가입/로그인/내 정보/로그아웃(블랙리스트) E2E 동작
- **환경 변수 기반 설정**: DB 접속·JWT 키·포트 등을 **코드 밖**으로 분리(12-Factor)
- **Swagger 연동**: 문서와 테스트를 한 화면에서 처리 가능
- **DB 문자셋 권장**: `utf8mb4` 적용 가이드(이모지·한글 안전)
- **DDL 전략 정리**: 초기 실험은 `create-drop`, 개발 안정화 땐 `update` 권장(데이터 유지)
- **로깅/디버깅 가이드**: Hikari/SQL 레벨 조정으로 원인 추적 수월

> (선택 개선) `users.email` / `blacklisted_token.token` **unique** 인덱스 명시  
> (선택 개선) `open-in-view: false`, 서버 응답 인코딩(UTF-8) 강제 옵션

---

## 6) 데이터 모델(ERD 요약)
```
users (id PK, email UNIQUE, password, name, phone, created_at, updated_at)
roles (id PK, name)
user_roles (user_id FK -> users.id, role_id FK -> roles.id)
blacklisted_token (id PK, token UNIQUE, expires_at)
```

---

## 8) 운영/품질 메모
- **보안**: 비밀값은 환경변수로만(커밋 금지), 액세스 토큰 만료 짧게 유지·로그아웃 블랙리스트로 보완
- **성능**: 인덱스/페치 전략으로 N+1 방지, 필요시 캐시 고려
- **문자셋**: DB/연결 모두 `utf8mb4` 권장
- **문서화**: Swagger UI 주소 README에 고정 표기, cURL 예시 유지
- **테스트**: 서비스/리포지토리/통합 테스트 점진 보강 권장

---

## 9) 트러블슈팅(자주 겪는 이슈)
- **DB 연결 실패**: `DB_URL/USERNAME/PASSWORD` 환경변수 확인, MySQL 기동·권한 확인
- **JWT 오류**: `JWT_SECRET_KEY` 32바이트 이상, 오타/공백 확인
- **403/401 구분**: 401=토큰 없음/무효, 403=토큰은 유효하나 **권한 부족** 또는 **블랙리스트**
- **한글 깨짐**: DB `utf8mb4`, 서버 인코딩 강제, 터미널 인코딩 확인
