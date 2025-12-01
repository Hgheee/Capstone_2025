-- 매칭 알고리즘을 위한 필드 추가 (안전 버전)
-- LostItem 테이블에 좌표 및 타입 필드 추가
-- 컬럼/인덱스가 이미 존재하면 에러를 무시합니다

-- 1. latitude 컬럼 추가 (에러 무시)
SET @sql = 'ALTER TABLE lost_item ADD COLUMN latitude DOUBLE NULL COMMENT ''위도 (매칭 알고리즘용)''';
SET @ignore = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
         WHERE TABLE_SCHEMA = DATABASE() 
         AND TABLE_NAME = 'lost_item' 
         AND COLUMN_NAME = 'latitude') > 0,
        'SELECT 1',
        @sql
    )
);
PREPARE stmt FROM @ignore;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. longitude 컬럼 추가 (에러 무시)
SET @sql = 'ALTER TABLE lost_item ADD COLUMN longitude DOUBLE NULL COMMENT ''경도 (매칭 알고리즘용)''';
SET @ignore = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
         WHERE TABLE_SCHEMA = DATABASE() 
         AND TABLE_NAME = 'lost_item' 
         AND COLUMN_NAME = 'longitude') > 0,
        'SELECT 1',
        @sql
    )
);
PREPARE stmt FROM @ignore;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. item_type 컬럼 추가 (에러 무시)
SET @sql = 'ALTER TABLE lost_item ADD COLUMN item_type VARCHAR(10) DEFAULT ''FOUND'' COMMENT ''분실물 타입 (LOST: 분실물, FOUND: 습득물)''';
SET @ignore = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
         WHERE TABLE_SCHEMA = DATABASE() 
         AND TABLE_NAME = 'lost_item' 
         AND COLUMN_NAME = 'item_type') > 0,
        'SELECT 1',
        @sql
    )
);
PREPARE stmt FROM @ignore;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 기존 데이터는 모두 FOUND 타입으로 설정
UPDATE lost_item SET item_type = 'FOUND' WHERE item_type IS NULL;

-- 5. 인덱스 추가 (에러 무시)
-- item_type 인덱스
SET @sql = 'CREATE INDEX idx_lost_item_item_type ON lost_item(item_type)';
SET @ignore = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
         WHERE TABLE_SCHEMA = DATABASE() 
         AND TABLE_NAME = 'lost_item' 
         AND INDEX_NAME = 'idx_lost_item_item_type') > 0,
        'SELECT 1',
        @sql
    )
);
PREPARE stmt FROM @ignore;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 좌표 복합 인덱스
SET @sql = 'CREATE INDEX idx_lost_item_coordinates ON lost_item(latitude, longitude)';
SET @ignore = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
         WHERE TABLE_SCHEMA = DATABASE() 
         AND TABLE_NAME = 'lost_item' 
         AND INDEX_NAME = 'idx_lost_item_coordinates') > 0,
        'SELECT 1',
        @sql
    )
);
PREPARE stmt FROM @ignore;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;





