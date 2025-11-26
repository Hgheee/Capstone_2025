-- item_type 컬럼 빠른 추가 (에러 무시)

-- item_type 컬럼 추가
ALTER TABLE lost_item 
ADD COLUMN item_type VARCHAR(10) DEFAULT 'FOUND';

-- 기존 데이터 업데이트
UPDATE lost_item SET item_type = 'FOUND' WHERE item_type IS NULL;

-- 인덱스 추가
CREATE INDEX idx_lost_item_item_type ON lost_item(item_type);


