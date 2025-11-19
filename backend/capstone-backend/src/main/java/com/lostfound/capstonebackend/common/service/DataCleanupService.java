package com.lostfound.capstonebackend.common.service;

import com.lostfound.capstonebackend.common.util.EncodingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 깨진 데이터를 자동으로 감지하고 삭제하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 깨진 데이터를 자동으로 감지하고 삭제합니다.
     * 
     * @return 삭제된 데이터 개수
     */
    @Transactional
    public int cleanupBrokenData() {
        log.info("========================================");
        log.info("깨진 데이터 자동 정리 시작");
        log.info("========================================");

        int totalDeleted = 0;

        // 1. 전체 데이터 조회 (title, description, category, location이 주요 대상)
        String selectSql = "SELECT id, title, description, category, location, color, datasource " +
                          "FROM lost_item " +
                          "WHERE title IS NOT NULL OR description IS NOT NULL OR category IS NOT NULL " +
                          "OR location IS NOT NULL OR color IS NOT NULL";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
        log.info("전체 데이터 {}건 검사 중...", rows.size());

        int checkedCount = 0;
        StringBuilder deleteSql = new StringBuilder("DELETE FROM lost_item WHERE id IN (");
        boolean hasItems = false;

        for (Map<String, Object> row : rows) {
            checkedCount++;
            
            Long id = ((Number) row.get("id")).longValue();
            String title = (String) row.get("title");
            String description = (String) row.get("description");
            String category = (String) row.get("category");
            String location = (String) row.get("location");
            String color = (String) row.get("color");
            String datasource = (String) row.get("datasource");

            // 깨진 데이터 감지
            boolean isTitleBroken = title != null && EncodingUtil.isBroken(title);
            boolean isDescriptionBroken = description != null && EncodingUtil.isBroken(description);
            boolean isCategoryBroken = category != null && EncodingUtil.isBroken(category);
            boolean isLocationBroken = location != null && EncodingUtil.isBroken(location);
            boolean isColorBroken = color != null && EncodingUtil.isBroken(color);

            if (isTitleBroken || isDescriptionBroken || isCategoryBroken || isLocationBroken || isColorBroken) {
                if (hasItems) {
                    deleteSql.append(", ");
                }
                deleteSql.append(id);
                hasItems = true;

                log.warn("🗑️ 깨진 데이터 발견 (ID: {}) - title: {}, desc: {}, category: {}, location: {}, color: {}, source: {}",
                        id,
                        isTitleBroken ? "❌" : "✅",
                        isDescriptionBroken ? "❌" : "✅",
                        isCategoryBroken ? "❌" : "✅",
                        isLocationBroken ? "❌" : "✅",
                        isColorBroken ? "❌" : "✅",
                        datasource);
                
                if (isTitleBroken && title != null) {
                    log.warn("   → 깨진 title: {}", EncodingUtil.safeSubstring(title, 50));
                }
            }

            // 진행 상황 로그 (1000건마다)
            if (checkedCount % 1000 == 0) {
                log.info("진행 중: {}/{} 건 검사 완료", checkedCount, rows.size());
            }
        }

        // 2. 깨진 데이터 일괄 삭제
        if (hasItems) {
            deleteSql.append(")");
            totalDeleted = jdbcTemplate.update(deleteSql.toString());
            log.info("✅ 깨진 데이터 {}건 삭제 완료!", totalDeleted);
        } else {
            log.info("✅ 깨진 데이터가 없습니다!");
        }

        log.info("========================================");
        log.info("깨진 데이터 정리 완료 - {}건 삭제", totalDeleted);
        log.info("========================================");

        return totalDeleted;
    }

    /**
     * 특정 날짜에 생성된 데이터를 삭제합니다.
     * 
     * @param targetDate 삭제할 날짜 (YYYY-MM-DD 형식)
     * @param dataSource 데이터 소스 (null이면 전체)
     * @return 삭제된 데이터 개수
     */
    @Transactional
    public int deleteByDate(String targetDate, String dataSource) {
        String sql;
        int deleted;

        if (dataSource != null && !dataSource.trim().isEmpty()) {
            sql = "DELETE FROM lost_item WHERE DATE(created_at) = ? AND datasource = ?";
            deleted = jdbcTemplate.update(sql, targetDate, dataSource);
            log.info("날짜 {} + 소스 {} 데이터 {}건 삭제", targetDate, dataSource, deleted);
        } else {
            sql = "DELETE FROM lost_item WHERE DATE(created_at) = ?";
            deleted = jdbcTemplate.update(sql, targetDate);
            log.info("날짜 {} 데이터 {}건 삭제", targetDate, deleted);
        }

        return deleted;
    }

    /**
     * ID 범위로 데이터를 삭제합니다.
     * 
     * @param startId 시작 ID
     * @param endId 종료 ID
     * @return 삭제된 데이터 개수
     */
    @Transactional
    public int deleteByIdRange(long startId, long endId) {
        String sql = "DELETE FROM lost_item WHERE id BETWEEN ? AND ?";
        int deleted = jdbcTemplate.update(sql, startId, endId);
        log.info("ID {} ~ {} 범위 데이터 {}건 삭제", startId, endId, deleted);
        return deleted;
    }

    /**
     * 전체 데이터 상태를 확인합니다.
     * 
     * @return 전체 데이터 개수, 데이터 소스별 개수, 깨진 데이터 예상 개수
     */
    public Map<String, Object> getDataStatus() {
        // 전체 개수
        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lost_item", Integer.class);

        // 데이터 소스별 개수
        List<Map<String, Object>> bySource = jdbcTemplate.queryForList(
                "SELECT datasource, COUNT(*) as count FROM lost_item GROUP BY datasource");

        // 최근 10개 데이터
        List<Map<String, Object>> recent = jdbcTemplate.queryForList(
                "SELECT id, SUBSTRING(title, 1, 50) as title, datasource, created_at " +
                "FROM lost_item ORDER BY created_at DESC LIMIT 10");

        return Map.of(
                "totalCount", totalCount != null ? totalCount : 0,
                "bySource", bySource,
                "recent", recent
        );
    }

    /**
     * 깨진 데이터의 예상 개수를 반환합니다. (정확하지 않을 수 있음)
     * 정규식으로 대략적인 개수를 확인합니다.
     * 
     * @return 깨진 데이터 예상 개수
     */
    public int estimateBrokenDataCount() {
        // 이상한 문자가 포함된 데이터 개수 (대략적)
        String sql = "SELECT COUNT(*) FROM lost_item WHERE " +
                    "title REGEXP '[^가-힣a-zA-Z0-9\\\\s\\\\-_.,!?()@#$%&*+=/\\\\[\\\\]{}:''\"<>~`|;]' OR " +
                    "description REGEXP '[^가-힣a-zA-Z0-9\\\\s\\\\-_.,!?()@#$%&*+=/\\\\[\\\\]{}:''\"<>~`|;]' OR " +
                    "category REGEXP '[^가-힣a-zA-Z0-9\\\\s\\\\-_.,!?()@#$%&*+=/\\\\[\\\\]{}:''\"<>~`|;]' OR " +
                    "location REGEXP '[^가-힣a-zA-Z0-9\\\\s\\\\-_.,!?()@#$%&*+=/\\\\[\\\\]{}:''\"<>~`|;]' OR " +
                    "color REGEXP '[^가-힣a-zA-Z0-9\\\\s\\\\-_.,!?()@#$%&*+=/\\\\[\\\\]{}:''\"<>~`|;]'";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}

