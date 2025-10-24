package com.lostfound.capstonebackend.domain.lostitem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 분실물 통계 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 각종 통계 정보 조회 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LostItemStatisticsService {

    private final LostItemRepository lostItemRepository;

    /**
     * 카테고리별 분실물 개수 통계를 조회합니다.
     * @return 카테고리별 개수 맵 (카테고리명 -> 개수)
     */
    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> categoryStats = lostItemRepository.countByCategory();
        return categoryStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    /**
     * 상태별 분실물 개수 통계를 조회합니다.
     * @return 상태별 개수 맵 (상태명 -> 개수)
     */
    public Map<String, Long> getStatusStatistics() {
        List<Object[]> statusStats = lostItemRepository.countByStatus();
        return statusStats.stream()
                .collect(Collectors.toMap(
                        row -> ((LostItem.Status) row[0]).name(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    /**
     * 데이터 소스별 분실물 개수 통계를 조회합니다.
     * @return 데이터소스별 개수 맵 (소스명 -> 개수)
     */
    public Map<String, Long> getDataSourceStatistics() {
        List<Object[]> dataSourceStats = lostItemRepository.countByDataSource();
        return dataSourceStats.stream()
                .collect(Collectors.toMap(
                        row -> ((LostItem.DataSource) row[0]).name(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    /**
     * 최고 인기 카테고리 TOP N을 조회합니다.
     * @param limit 조회할 상위 개수
     * @return 상위 카테고리 맵 (카테고리명 -> 개수)
     */
    public Map<String, Long> getTopCategories(int limit) {
        // Pageable을 사용하여 limit 처리
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> topCategories = lostItemRepository.findTopCategoriesByCount(pageable);
        return topCategories.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (oldValue, newValue) -> newValue,
                        java.util.LinkedHashMap::new // 순서 유지
                ));
    }

    /**
     * 월별 등록 통계를 조회합니다. (최근 12개월)
     * @return 월별 통계 맵 (년월 -> 개수)
     */
    public Map<String, Long> getMonthlyStatistics() {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(12);
        List<Object[]> monthlyStats = lostItemRepository.getMonthlyStatistics(startDate);

        return monthlyStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (oldValue, newValue) -> newValue,
                        java.util.LinkedHashMap::new // 순서 유지
                ));
    }

    /**
     * 특정 기간 동안 등록된 분실물의 총 개수를 계산합니다.
     * @param startDate 조회 시작 일시
     * @param endDate 조회 종료 일시
     * @return 해당 기간에 등록된 분실물 총 개수
     */
    public Long getCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return lostItemRepository.countByCreatedAtBetween(startDate, endDate);
    }

    /**
     * 전체 통계 정보를 종합하여 조회합니다.
     * @return 종합 통계 정보 맵
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> overallStats = new java.util.HashMap<>();

        // 기본 통계
        overallStats.put("totalItems", lostItemRepository.count());
        overallStats.put("categoryStats", getCategoryStatistics());
        overallStats.put("statusStats", getStatusStatistics());
        overallStats.put("dataSourceStats", getDataSourceStatistics());

        // 인기 카테고리 TOP 5
        overallStats.put("topCategories", getTopCategories(5));

        // 월별 통계
        overallStats.put("monthlyStats", getMonthlyStatistics());

        // 최근 30일 등록 수
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        overallStats.put("recentItemsCount", getCountByDateRange(thirtyDaysAgo, LocalDateTime.now()));

        // 최근 7일 등록 수
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        overallStats.put("weeklyItemsCount", getCountByDateRange(sevenDaysAgo, LocalDateTime.now()));

        log.info("전체 통계 정보 조회 완료 - 총 분실물: {}", overallStats.get("totalItems"));

        return overallStats;
    }
}
