-- item_type 컬럼 확인 및 추가

-- 1. item_type 컬럼 존재 여부 확인
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'capstone_db'
  AND TABLE_NAME = 'lost_item'
  AND COLUMN_NAME = 'item_type';

-- 2. item_type 컬럼이 없으면 추가
-- (MySQL Workbench에서 위 쿼리 결과가 비어있으면 아래 실행)
ALTER TABLE lost_item 
ADD COLUMN item_type VARCHAR(10) DEFAULT 'FOUND' COMMENT '분실물 타입 (LOST: 분실물, FOUND: 습득물)';

-- 3. 기존 데이터의 item_type이 NULL이면 'FOUND'로 설정
UPDATE lost_item SET item_type = 'FOUND' WHERE item_type IS NULL;

-- 4. 인덱스 추가 (없으면)
CREATE INDEX idx_lost_item_item_type ON lost_item(item_type);

-- 5. 최종 확인
DESCRIBE lost_item;


