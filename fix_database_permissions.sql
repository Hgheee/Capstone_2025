-- MySQL 권한 수정 스크립트 (root로 실행)

-- 1. 데이터베이스 확인 및 생성
CREATE DATABASE IF NOT EXISTS capstonedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 사용자 확인 및 생성
CREATE USER IF NOT EXISTS 'hogeonhee'@'localhost' IDENTIFIED BY '0316';

-- 3. 모든 권한 부여
GRANT ALL PRIVILEGES ON capstonedb.* TO 'hogeonhee'@'localhost';

-- 4. 권한 즉시 적용
FLUSH PRIVILEGES;

-- 5. 권한 확인
SHOW GRANTS FOR 'hogeonhee'@'localhost';

SELECT 'Database permissions fixed!' as message;

