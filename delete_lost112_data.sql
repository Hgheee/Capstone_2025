-- ========================================
-- LOST112 데이터만 삭제
-- (서울교통공사 데이터는 유지)
-- ========================================

USE capstone_db;

-- 삭제 전 현황 확인
SELECT 
    datasource,
    COUNT(*) as count
FROM lost_item
GROUP BY datasource;

-- LOST112 데이터만 삭제
DELETE FROM lost_item 
WHERE datasource = 'LOST112';

-- 삭제 후 현황 확인
SELECT 
    datasource,
    COUNT(*) as count
FROM lost_item
GROUP BY datasource;

SELECT CONCAT('✅ LOST112 데이터 삭제 완료! 서울교통공사 데이터는 ', COUNT(*), '건 유지됨') as result
FROM lost_item
WHERE datasource = 'SEOUL_LOST';



