-- 데이터베이스 컬럼 확인 쿼리

-- 1. lost_item 테이블 구조 확인
DESCRIBE lost_item;

-- 2. 특정 컬럼 존재 여부 확인
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'capstone_db'
  AND TABLE_NAME = 'lost_item'
  AND COLUMN_NAME IN ('latitude', 'longitude', 'item_type');

-- 3. 모든 컬럼 목록
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'capstone_db'
  AND TABLE_NAME = 'lost_item'
ORDER BY ORDINAL_POSITION;


