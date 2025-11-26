-- 분실물 테이블에 region 필드 추가 및 인덱스 생성
-- 작성일: 2025-11-05
-- 목적: 지역별 검색 기능 개선을 위한 region 필드 추가

-- Step 1: region 컬럼 추가
ALTER TABLE lost_item 
ADD COLUMN region VARCHAR(20) NULL COMMENT '지역 (서울시 구)';

-- Step 2: 인덱스 생성 (검색 성능 향상)
CREATE INDEX idx_lost_item_region ON lost_item(region);

-- Step 3: 기존 데이터에서 지역 정보 추출 및 업데이트
-- 강남권
UPDATE lost_item SET region = '강남구' WHERE region IS NULL AND (location LIKE '%강남구%' OR storage_location LIKE '%강남구%');
UPDATE lost_item SET region = '서초구' WHERE region IS NULL AND (location LIKE '%서초구%' OR storage_location LIKE '%서초구%');
UPDATE lost_item SET region = '송파구' WHERE region IS NULL AND (location LIKE '%송파구%' OR storage_location LIKE '%송파구%');
UPDATE lost_item SET region = '강동구' WHERE region IS NULL AND (location LIKE '%강동구%' OR storage_location LIKE '%강동구%');

-- 강서권
UPDATE lost_item SET region = '강서구' WHERE region IS NULL AND (location LIKE '%강서구%' OR storage_location LIKE '%강서구%');
UPDATE lost_item SET region = '양천구' WHERE region IS NULL AND (location LIKE '%양천구%' OR storage_location LIKE '%양천구%');
UPDATE lost_item SET region = '구로구' WHERE region IS NULL AND (location LIKE '%구로구%' OR storage_location LIKE '%구로구%');
UPDATE lost_item SET region = '금천구' WHERE region IS NULL AND (location LIKE '%금천구%' OR storage_location LIKE '%금천구%');

-- 동북권
UPDATE lost_item SET region = '노원구' WHERE region IS NULL AND (location LIKE '%노원구%' OR storage_location LIKE '%노원구%');
UPDATE lost_item SET region = '도봉구' WHERE region IS NULL AND (location LIKE '%도봉구%' OR storage_location LIKE '%도봉구%');
UPDATE lost_item SET region = '강북구' WHERE region IS NULL AND (location LIKE '%강북구%' OR storage_location LIKE '%강북구%');
UPDATE lost_item SET region = '성북구' WHERE region IS NULL AND (location LIKE '%성북구%' OR storage_location LIKE '%성북구%');

-- 동남권
UPDATE lost_item SET region = '광진구' WHERE region IS NULL AND (location LIKE '%광진구%' OR storage_location LIKE '%광진구%');
UPDATE lost_item SET region = '성동구' WHERE region IS NULL AND (location LIKE '%성동구%' OR storage_location LIKE '%성동구%');
UPDATE lost_item SET region = '동대문구' WHERE region IS NULL AND (location LIKE '%동대문구%' OR storage_location LIKE '%동대문구%');
UPDATE lost_item SET region = '중랑구' WHERE region IS NULL AND (location LIKE '%중랑구%' OR storage_location LIKE '%중랑구%');

-- 서북권
UPDATE lost_item SET region = '은평구' WHERE region IS NULL AND (location LIKE '%은평구%' OR storage_location LIKE '%은평구%');
UPDATE lost_item SET region = '서대문구' WHERE region IS NULL AND (location LIKE '%서대문구%' OR storage_location LIKE '%서대문구%');
UPDATE lost_item SET region = '마포구' WHERE region IS NULL AND (location LIKE '%마포구%' OR storage_location LIKE '%마포구%');

-- 중심권
UPDATE lost_item SET region = '종로구' WHERE region IS NULL AND (location LIKE '%종로구%' OR storage_location LIKE '%종로구%');
UPDATE lost_item SET region = '중구' WHERE region IS NULL AND (location LIKE '%중구%' OR storage_location LIKE '%중구%');
UPDATE lost_item SET region = '용산구' WHERE region IS NULL AND (location LIKE '%용산구%' OR storage_location LIKE '%용산구%');

-- 남부권
UPDATE lost_item SET region = '영등포구' WHERE region IS NULL AND (location LIKE '%영등포구%' OR storage_location LIKE '%영등포구%');
UPDATE lost_item SET region = '동작구' WHERE region IS NULL AND (location LIKE '%동작구%' OR storage_location LIKE '%동작구%');
UPDATE lost_item SET region = '관악구' WHERE region IS NULL AND (location LIKE '%관악구%' OR storage_location LIKE '%관악구%');

-- 업데이트 결과 확인
SELECT 
    region,
    COUNT(*) as count
FROM lost_item
GROUP BY region
ORDER BY count DESC;

-- 지역 정보가 없는 데이터 확인
SELECT COUNT(*) as no_region_count
FROM lost_item
WHERE region IS NULL;









