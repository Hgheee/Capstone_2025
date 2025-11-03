-- ==============================================
-- 데이터베이스 확인 및 수정 스크립트
-- ==============================================

-- 1. 데이터베이스 선택
USE capstonedb;

-- 2. 테이블 존재 확인
SHOW TABLES;

-- 3. lost_item 테이블 구조 확인
DESCRIBE lost_item;

-- 4. 현재 데이터 개수 확인
SELECT COUNT(*) as total_count FROM lost_item;
SELECT '현재 분실물 데이터 개수:' as message;

-- 5. 기존 데이터 확인
SELECT id, title, category, location, status FROM lost_item;

-- 6. 데이터가 없으면 샘플 데이터 삽입
-- 기존 데이터 삭제 후 재삽입 (선택사항)
-- DELETE FROM lost_item WHERE datasource = 'USER';

-- 샘플 데이터 삽입 (중복 방지)
INSERT INTO lost_item (title, description, category, location, found_date, status, color, datasource, user_id)
SELECT * FROM (
    SELECT '검은색 지갑' as title, '신한은행 카드가 들어있는 가죽 지갑' as description, '지갑' as category, '강남역 2번 출구' as location, '2025-01-15' as found_date, 'FOUND' as status, '검은색' as color, 'USER' as datasource, 1 as user_id
    UNION ALL
    SELECT '빨간색 우산', '접이식 자동 우산', '우산', '서초구 서초대로', '2025-01-14', 'FOUND', '빨간색', 'USER', 1
    UNION ALL
    SELECT '아이폰 15', '흰색 실리콘 케이스 장착', '핸드폰', '강남구 테헤란로', '2025-01-13', 'FOUND', '흰색', 'USER', 1
    UNION ALL
    SELECT '노트북 가방', '검은색 백팩 형태의 노트북 가방', '가방', '송파구 올림픽로', '2025-01-12', 'FOUND', '검은색', 'USER', 1
    UNION ALL
    SELECT '에어팟', '에어팟 프로 2세대 케이스', '귀중품', '강동구 천호역', '2025-01-11', 'FOUND', '흰색', 'USER', 1
    UNION ALL
    SELECT '삼성 갤럭시', '검은색 케이스 장착된 갤럭시 S24', '핸드폰', '서울시 송파구', '2025-01-10', 'FOUND', '검은색', 'USER', 1
    UNION ALL
    SELECT '에어팟 맥스', '실버 색상 헤드폰', '귀중품', '강남구 역삼역', '2025-01-09', 'FOUND', '실버', 'USER', 1
    UNION ALL
    SELECT '신분증', '주민등록증', '서류', '마포구 홍대입구역', '2025-01-08', 'FOUND', NULL, 'USER', 1
    UNION ALL
    SELECT '맥북 프로', '16인치 M3 Pro', '노트북', '강남구 선릉역', '2025-01-07', 'FOUND', '스페이스 그레이', 'USER', 1
    UNION ALL
    SELECT '패딩 점퍼', '노스페이스 검은색', '의류', '서초구 강남역', '2025-01-06', 'FOUND', '검은색', 'USER', 1
    UNION ALL
    SELECT '현대카드', '현대카드 M', '귀중품', '강남구 삼성역', '2025-01-05', 'FOUND', '검은색', 'USER', 1
    UNION ALL
    SELECT '에어팟 케이스', '에어팟 2세대', '귀중품', '송파구 잠실역', '2025-01-04', 'FOUND', '흰색', 'USER', 1
    UNION ALL
    SELECT '손목시계', '롤렉스', '귀중품', '강남구 논현역', '2025-01-03', 'FOUND', '금색', 'USER', 1
    UNION ALL
    SELECT '백팩', '검은색 나이키 백팩', '가방', '서초구 교대역', '2025-01-02', 'FOUND', '검은색', 'USER', 1
    UNION ALL
    SELECT '여권', '대한민국 여권', '서류', '강남구 강남역', '2025-01-01', 'FOUND', '남색', 'USER', 1
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM lost_item WHERE title = tmp.title AND location = tmp.location
);

-- 7. 최종 데이터 개수 확인
SELECT COUNT(*) as final_count FROM lost_item;
SELECT '최종 분실물 데이터 개수:' as message;

-- 8. 카테고리별 개수 확인
SELECT category, COUNT(*) as count 
FROM lost_item 
GROUP BY category 
ORDER BY count DESC;

-- 9. 최근 등록된 분실물 확인 (상위 10개)
SELECT id, title, category, location, found_date, status, created_at
FROM lost_item
ORDER BY created_at DESC
LIMIT 10;

SELECT '데이터베이스 확인 및 수정 완료!' as message;

