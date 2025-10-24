package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 분실물 통계 정보를 제공하는 REST 컨트롤러입니다.
 * 관리자나 일반 사용자가 다양한 통계 정보를 조회할 수 있는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/lost-items/statistics")
@RequiredArgsConstructor
@Tag(name = "분실물 통계", description = "분실물 관련 통계 정보 조회 API")
public class LostItemStatisticsController {

    private final LostItemStatisticsService statisticsService;

    /**
     * 카테고리별 분실물 개수 통계를 조회합니다.
     * @return 카테고리별 개수 통계가 포함된 ApiResponse
     */
    @GetMapping("/categories")
    @Operation(summary = "카테고리별 통계", description = "각 카테고리별 분실물 개수 조회")
    public ApiResponse<Map<String, Long>> getCategoryStatistics() {
        return ApiResponse.ok(statisticsService.getCategoryStatistics());
    }

    /**
     * 상태별 분실물 개수 통계를 조회합니다.
     * @return 상태별 개수 통계가 포함된 ApiResponse
     */
    @GetMapping("/status")
    @Operation(summary = "상태별 통계", description = "각 상태별 분실물 개수 조회 (FOUND, CLAIMED, EXPIRED)")
    public ApiResponse<Map<String, Long>> getStatusStatistics() {
        return ApiResponse.ok(statisticsService.getStatusStatistics());
    }

    /**
     * 데이터 소스별 분실물 개수 통계를 조회합니다.
     * @return 데이터소스별 개수 통계가 포함된 ApiResponse
     */
    @GetMapping("/datasource")
    @Operation(summary = "데이터소스별 통계", description = "사용자 등록 vs LOST112 데이터 개수 비교")
    public ApiResponse<Map<String, Long>> getDataSourceStatistics() {
        return ApiResponse.ok(statisticsService.getDataSourceStatistics());
    }

    /**
     * 최고 인기 카테고리 TOP N을 조회합니다.
     * @param limit 조회할 상위 개수 (기본값: 10)
     * @return 상위 카테고리 통계가 포함된 ApiResponse
     */
    @GetMapping("/top-categories")
    @Operation(summary = "인기 카테고리 TOP N", description = "가장 많이 등록된 카테고리 순위")
    public ApiResponse<Map<String, Long>> getTopCategories(
            @Parameter(description = "조회할 상위 개수") @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(statisticsService.getTopCategories(limit));
    }

    /**
     * 월별 등록 통계를 조회합니다. (최근 12개월)
     * @return 월별 등록 통계가 포함된 ApiResponse
     */
    @GetMapping("/monthly")
    @Operation(summary = "월별 등록 통계", description = "최근 12개월간 월별 분실물 등록 개수")
    public ApiResponse<Map<String, Long>> getMonthlyStatistics() {
        return ApiResponse.ok(statisticsService.getMonthlyStatistics());
    }

    /**
     * 특정 기간 동안 등록된 분실물의 총 개수를 계산합니다.
     * @param startDate 조회 시작 일시 (yyyy-MM-dd'T'HH:mm:ss)
     * @param endDate   조회 종료 일시 (yyyy-MM-dd'T'HH:mm:ss)
     * @return 해당 기간에 등록된 분실물 총 개수가 포함된 ApiResponse
     */
    @GetMapping("/count-by-range")
    @Operation(summary = "기간별 등록 개수", description = "특정 기간 동안 등록된 분실물의 총 개수 조회")
    public ApiResponse<Long> getCountByDateRange(
            @Parameter(description = "시작 일시 (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 일시 (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ApiResponse.ok(statisticsService.getCountByDateRange(startDate, endDate));
    }

    /**
     * 전체 통계 정보를 종합하여 조회합니다.
     * 관리자 대시보드나 메인 페이지에서 사용할 수 있는 종합 통계 정보를 제공합니다.
     * @return 종합 통계 정보가 포함된 ApiResponse
     */
    @GetMapping("/overall")
    @Operation(summary = "종합 통계", description = "전체 분실물 통계 정보 종합 (대시보드용)")
    public ApiResponse<Map<String, Object>> getOverallStatistics() {
        return ApiResponse.ok(statisticsService.getOverallStatistics());
    }
}