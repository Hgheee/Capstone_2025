package com.lostfound.capstonebackend.domain.lostitem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LostItemStatisticsServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @InjectMocks
    private LostItemStatisticsService statisticsService;

    @Test
    @DisplayName("카테고리 통계 조회")
    void getCategoryStatistics() {
        List<Object[]> stats = java.util.Collections.singletonList(new Object[]{"전자", 5L});
        given(lostItemRepository.countByCategory()).willReturn(stats);
        Map<String, Long> result = statisticsService.getCategoryStatistics();
        assertThat(result).containsEntry("전자", 5L);
    }

    @Test
    @DisplayName("상태별 통계 조회")
    void getStatusStatistics() {
        List<Object[]> statusStats = java.util.Collections.singletonList(new Object[]{LostItem.Status.FOUND, 3L});
        given(lostItemRepository.countByStatus()).willReturn(statusStats);
        Map<String, Long> result = statisticsService.getStatusStatistics();
        assertThat(result).containsEntry("FOUND", 3L);
    }

    @Test
    @DisplayName("데이터 소스 통계")
    void getDataSourceStatistics() {
        List<Object[]> sourceStats = java.util.Collections.singletonList(new Object[]{LostItem.DataSource.USER, 4L});
        given(lostItemRepository.countByDataSource()).willReturn(sourceStats);
        Map<String, Long> result = statisticsService.getDataSourceStatistics();
        assertThat(result).containsEntry("USER", 4L);
    }

    @Test
    @DisplayName("인기 카테고리 TOP N")
    void getTopCategories() {
        java.util.List<Object[]> topCategories = new java.util.ArrayList<>();
        topCategories.add(new Object[]{"지갑", 7L});
        topCategories.add(new Object[]{"휴대폰", 5L});
        given(lostItemRepository.findTopCategoriesByCount(eq(PageRequest.of(0, 3))))
                .willReturn(topCategories);

        Map<String, Long> result = statisticsService.getTopCategories(3);
        assertThat(result.keySet()).containsExactly("지갑", "휴대폰");
    }

    @Test
    @DisplayName("월별 통계")
    void getMonthlyStatistics() {
        List<Object[]> monthlyStats = java.util.Collections.singletonList(new Object[]{"202401", 10L});
        given(lostItemRepository.getMonthlyStatistics(any(LocalDateTime.class)))
                .willReturn(monthlyStats);

        Map<String, Long> result = statisticsService.getMonthlyStatistics();
        assertThat(result).containsEntry("202401", 10L);
    }

    @Test
    @DisplayName("기간별 등록 개수")
    void getCountByDateRange() {
        given(lostItemRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(12L);

        Long result = statisticsService.getCountByDateRange(LocalDateTime.now().minusDays(7), LocalDateTime.now());
        assertThat(result).isEqualTo(12L);
    }

    @Test
    @DisplayName("종합 통계")
    void getOverallStatistics() {
        given(lostItemRepository.count()).willReturn(20L);
        given(lostItemRepository.countByCategory()).willReturn(java.util.Collections.singletonList(new Object[]{"지갑", 5L}));
        given(lostItemRepository.countByStatus()).willReturn(java.util.Collections.singletonList(new Object[]{LostItem.Status.FOUND, 10L}));
        given(lostItemRepository.countByDataSource()).willReturn(java.util.Collections.singletonList(new Object[]{LostItem.DataSource.USER, 15L}));
        List<Object[]> topFive = java.util.Collections.singletonList(new Object[]{"지갑", 5L});
        List<Object[]> overallMonthly = java.util.Collections.singletonList(new Object[]{"202401", 2L});
        given(lostItemRepository.findTopCategoriesByCount(eq(PageRequest.of(0, 5)))).willReturn(topFive);
        given(lostItemRepository.getMonthlyStatistics(any(LocalDateTime.class))).willReturn(overallMonthly);
        given(lostItemRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

        Map<String, Object> result = statisticsService.getOverallStatistics();
        assertThat(result).containsKeys("totalItems", "categoryStats", "statusStats", "dataSourceStats", "monthlyStats", "recentItemsCount", "weeklyItemsCount");
    }
}
