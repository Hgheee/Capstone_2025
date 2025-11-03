# 데이터베이스 설정 및 실행 가이드

## 📊 데이터베이스 정보

- **데이터베이스 이름**: `capstonedb`
- **사용자**: `hogeonhee`
- **비밀번호**: `0316`
- **포트**: `3306` (기본 MySQL 포트)

## 🚀 1단계: 데이터베이스 생성

MySQL에 접속하여 데이터베이스를 생성합니다.

### Windows

```bash
# MySQL 접속
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'hogeonhee'@'localhost' IDENTIFIED BY '0316';
GRANT ALL PRIVILEGES ON capstonedb.* TO 'hogeonhee'@'localhost';
FLUSH PRIVILEGES;

# 데이터베이스 선택
USE capstonedb;

# 종료
EXIT;
```

### 사용자가 이미 존재하는 경우

```sql
# 기존 사용자 삭제 후 재생성
DROP USER IF EXISTS 'hogeonhee'@'localhost';
CREATE USER 'hogeonhee'@'localhost' IDENTIFIED BY '0316';
GRANT ALL PRIVILEGES ON capstonedb.* TO 'hogeonhee'@'localhost';
FLUSH PRIVILEGES;
```

## 🔧 2단계: 스키마 자동 생성 (Spring Boot)

백엔드를 실행하면 스키마가 자동으로 생성됩니다.

### application.yml 설정 확인

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 스키마 자동 업데이트
  sql:
    init:
      mode: always  # schema.sql 자동 실행
```

### 백엔드 실행

```bash
cd backend/capstone-backend
./mvnw spring-boot:run
```

Windows에서는:
```bash
mvnw.cmd spring-boot:run
```

## 📋 3단계: 스키마 확인

### MySQL에서 테이블 확인

```bash
mysql -u hogeonhee -p capstonedb
```

비밀번호 입력: `0316`

```sql
-- 테이블 목록 확인
SHOW TABLES;

-- 결과:
-- +----------------------+
-- | Tables_in_capstonedb |
-- +----------------------+
-- | blacklisted_token    |
-- | lost_item            |
-- | users                |
-- +----------------------+

-- lost_item 테이블 구조 확인
DESCRIBE lost_item;

-- 데이터 확인
SELECT * FROM lost_item;
SELECT * FROM users;
```

## 📝 4단계: 샘플 데이터 확인

`schema.sql`에 포함된 샘플 데이터:

### 사용자 데이터
- 테스트 사용자: test@example.com (비밀번호: password)
- 관리자: admin@example.com (비밀번호: password)

### 분실물 데이터
- 검은색 지갑 (강남역)
- 빨간색 우산 (서초구)
- 아이폰 15 (강남구)
- 노트북 가방 (송파구)
- 에어팟 (강동구)

## 🔍 5단계: 데이터 조회 테스트

### SQL 쿼리로 확인

```sql
-- 모든 분실물 조회
SELECT id, title, category, location, status FROM lost_item;

-- 특정 카테고리 검색
SELECT * FROM lost_item WHERE category = '지갑';

-- 특정 지역 검색
SELECT * FROM lost_item WHERE location LIKE '%강남%';

-- 최근 등록된 분실물 (최신 5개)
SELECT * FROM lost_item ORDER BY created_at DESC LIMIT 5;
```

## 🐛 문제 해결

### 1. 데이터베이스 연결 실패

**오류**: `Communications link failure`

**해결**:
```bash
# MySQL 서비스 상태 확인
# Windows
net start MySQL80

# MySQL이 3306 포트에서 실행 중인지 확인
netstat -an | findstr 3306
```

### 2. 사용자 인증 실패

**오류**: `Access denied for user 'hogeonhee'@'localhost'`

**해결**:
```sql
-- MySQL root로 접속
mysql -u root -p

-- 사용자 권한 재설정
GRANT ALL PRIVILEGES ON capstonedb.* TO 'hogeonhee'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 스키마가 생성되지 않음

**해결**:
1. `application.yml` 확인
   - `ddl-auto: update` 설정 확인
   - `sql.init.mode: always` 설정 확인

2. `schema.sql` 파일 위치 확인
   - 위치: `src/main/resources/schema.sql`

3. 백엔드 로그 확인
   ```bash
   # 로그에서 스키마 관련 메시지 확인
   # "Executing SQL script from URL [file:...schema.sql]"
   ```

### 4. 한글 데이터 깨짐

**해결**:
```sql
-- 데이터베이스 문자셋 확인
SHOW VARIABLES LIKE 'character_set%';

-- 데이터베이스 문자셋 변경
ALTER DATABASE capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 테이블 문자셋 변경
ALTER TABLE lost_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 🔄 데이터베이스 초기화

완전히 새로 시작하려면:

```sql
-- 1. 데이터베이스 삭제
DROP DATABASE IF EXISTS capstonedb;

-- 2. 데이터베이스 재생성
CREATE DATABASE capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 백엔드 재실행 (스키마 자동 생성)
```

## 📊 테이블 구조

### lost_item 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | BIGINT | 기본키 (자동 증가) |
| title | VARCHAR(100) | 분실물 제목 |
| description | TEXT | 상세 설명 |
| category | VARCHAR(50) | 카테고리 |
| location | VARCHAR(200) | 습득 장소 |
| storage_location | VARCHAR(200) | 보관 장소 |
| found_date | DATE | 습득 날짜 |
| status | VARCHAR(20) | 상태 (FOUND, CLAIMED, 등) |
| external_id | VARCHAR(100) | 외부 시스템 ID |
| datasource | VARCHAR(20) | 데이터 출처 |
| view_count | INT | 조회수 |
| received_date | TIMESTAMP | 수령 일시 |
| color | VARCHAR(50) | 색상 |
| image_path | VARCHAR(255) | 이미지 경로 |
| user_id | BIGINT | 등록 사용자 ID |
| created_at | TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | 수정 일시 |

### 인덱스

- `idx_lost_item_status`: status 컬럼 인덱스
- `idx_lost_item_category`: category 컬럼 인덱스
- `idx_lost_item_found_date`: found_date 컬럼 인덱스
- `idx_lost_item_created_at`: created_at 컬럼 인덱스

## 🎯 다음 단계

데이터베이스 설정이 완료되면:

1. ✅ 백엔드 서버 실행
2. ✅ 프론트엔드 서버 실행
3. ✅ 브라우저에서 http://localhost:5173 접속
4. ✅ 검색 기능 테스트

자세한 내용은 `INTEGRATION_TEST_GUIDE.md` 참조

