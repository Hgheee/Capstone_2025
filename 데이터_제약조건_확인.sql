-- 데이터베이스 제약 조건 확인 쿼리

-- 1. lost_item 테이블 구조 확인
DESCRIBE lost_item;

-- 2. NULL 값 확인
SELECT 
    COUNT(*) as total,
    COUNT(latitude) as has_latitude,
    COUNT(longitude) as has_longitude,
    COUNT(item_type) as has_item_type,
    COUNT(CASE WHEN item_type IS NULL THEN 1 END) as null_item_type
FROM lost_item;

-- 3. item_type 값 분포 확인
SELECT 
    item_type,
    COUNT(*) as count
FROM lost_item
GROUP BY item_type;

-- 4. 잘못된 item_type 값 확인 (LOST, FOUND가 아닌 값)
SELECT 
    id,
    title,
    item_type
FROM lost_item
WHERE item_type NOT IN ('LOST', 'FOUND') OR item_type IS NULL
LIMIT 10;

-- 5. 외래키 제약 조건 확인
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'capstone_db'
  AND TABLE_NAME = 'lost_item'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 6. 유니크 제약 조건 확인
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'capstone_db'
  AND TABLE_NAME = 'lost_item'
  AND CONSTRAINT_NAME LIKE 'UK_%' OR CONSTRAINT_NAME LIKE 'UX_%';


