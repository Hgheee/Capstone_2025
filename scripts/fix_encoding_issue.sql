-- ==========================================
-- 한글 깨진 데이터 확인 및 삭제 스크립트
-- ==========================================

-- 1. 깨진 데이터 확인 (실행 전 확인용)
SELECT 
    id, 
    title, 
    status,
    datasource,
    location,
    storage_location,
    created_at
FROM lost_item
WHERE 
    -- 한글 깨짐 패턴 (깨진 문자 포함)
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%'
ORDER BY created_at DESC
LIMIT 100;

-- 2. 깨진 데이터 개수 확인
SELECT 
    datasource,
    status,
    COUNT(*) as broken_count
FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%'
GROUP BY datasource, status;

-- 3. "습득" 상태의 데이터만 확인
SELECT 
    id,
    title,
    datasource,
    location,
    created_at
FROM lost_item
WHERE status = 'STORED'  -- 습득 = STORED
    AND (
        title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
        OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    )
ORDER BY created_at DESC
LIMIT 50;

-- ==========================================
-- 깨진 데이터 삭제 (주의: 백업 후 실행!)
-- ==========================================

-- 4. 백업 테이블 생성 (선택사항, 안전을 위해 권장)
CREATE TABLE IF NOT EXISTS lost_item_backup_20251105 AS
SELECT * FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%';

-- 5. 깨진 데이터 삭제 실행
DELETE FROM lost_item
WHERE 
    title LIKE '%뿉%' OR title LIKE '%뼱%' OR title LIKE '%뙚%' 
    OR title LIKE '%媛%' OR title LIKE '%뺣%' OR title LIKE '%룞%'
    OR location LIKE '%뿉%' OR location LIKE '%뼱%' OR location LIKE '%뙚%'
    OR storage_location LIKE '%뿉%' OR storage_location LIKE '%뼱%';

-- 6. 삭제 후 확인
SELECT 
    datasource,
    status,
    COUNT(*) as remaining_count
FROM lost_item
GROUP BY datasource, status;

-- 7. 전체 데이터 개수 확인
SELECT COUNT(*) as total_items FROM lost_item;

