-- ================================================
-- external_id에 UNIQUE 제약 조건 추가
-- (중복 데이터 방지를 위함)
-- ================================================

-- 1. 기존 중복 데이터 확인
SELECT 
    external_id, 
    COUNT(*) as duplicate_count,
    datasource
FROM lost_item
WHERE external_id IS NOT NULL
GROUP BY external_id, datasource
HAVING COUNT(*) > 1;

-- 2. 중복 데이터 정리 (오래된 것만 남기고 삭제)
DELETE l1 FROM lost_item l1
INNER JOIN lost_item l2 
WHERE 
    l1.external_id = l2.external_id
    AND l1.id > l2.id
    AND l1.external_id IS NOT NULL;

-- 3. UNIQUE 제약 조건 추가
ALTER TABLE lost_item 
ADD UNIQUE INDEX idx_unique_external_id (external_id);

-- 4. 결과 확인
SHOW INDEX FROM lost_item WHERE Key_name = 'idx_unique_external_id';



