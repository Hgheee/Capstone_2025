-- 매칭 알고리즘을 위한 필드 추가
-- LostItem 테이블에 좌표 및 타입 필드 추가
-- MySQL 5.7+ 호환 버전 (IF NOT EXISTS 대신 에러 무시 방식)

-- 1. latitude 컬럼 추가
ALTER TABLE lost_item 
ADD COLUMN latitude DOUBLE NULL COMMENT '위도 (매칭 알고리즘용)';

-- 2. longitude 컬럼 추가
ALTER TABLE lost_item 
ADD COLUMN longitude DOUBLE NULL COMMENT '경도 (매칭 알고리즘용)';

-- 3. item_type 컬럼 추가
ALTER TABLE lost_item 
ADD COLUMN item_type VARCHAR(10) DEFAULT 'FOUND' COMMENT '분실물 타입 (LOST: 분실물, FOUND: 습득물)';

-- 4. 기존 데이터는 모두 FOUND 타입으로 설정
UPDATE lost_item SET item_type = 'FOUND' WHERE item_type IS NULL;

-- 5. 인덱스 추가
-- item_type 인덱스
CREATE INDEX idx_lost_item_item_type ON lost_item(item_type);

-- 좌표 복합 인덱스
CREATE INDEX idx_lost_item_coordinates ON lost_item(latitude, longitude);

