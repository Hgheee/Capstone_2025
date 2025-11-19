package com.lostfound.capstonebackend.domain.admin.service;

import com.lostfound.capstonebackend.common.service.DataCleanupService;
import com.lostfound.capstonebackend.domain.admin.dto.IntegratedImportRequest;
import com.lostfound.capstonebackend.domain.lost112.SimpleLost112ImportService;
import com.lostfound.capstonebackend.domain.seoul.SeoulLostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LOST112와 서울교통공사 데이터를 통합하여 수집하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegratedDataCollectionService {

    private final SimpleLost112ImportService lost112ImportService;
    private final SeoulLostService seoulLostService;
    private final DataCleanupService dataCleanupService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * LOST112 + 서울교통공사 데이터를 한 번에 수집합니다.
     * 
     * @param request 수집 요청 (날짜 범위, 수집 대상 등)
     * @return 수집 결과 (각 소스별 수집 건수, 소요 시간 등)
     */
    public Map<String, Object> collectIntegratedData(IntegratedImportRequest request) {
        log.info("========================================");
        log.info("통합 데이터 수집 시작");
        log.info("========================================");
        log.info("요청 정보: {}", request);

        LocalDateTime startTime = LocalDateTime.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", startTime.format(FORMATTER));
        result.put("request", request);

        int totalCollected = 0;
        int totalBrokenCleaned = 0;

        try {
            // 1. LOST112 데이터 수집
            if (Boolean.TRUE.equals(request.getCollectLost112())) {
                log.info("📥 LOST112 데이터 수집 시작...");
                Map<String, Object> lost112Result = collectLost112Data(request);
                result.put("lost112", lost112Result);
                
                Integer collected = (Integer) lost112Result.get("newlySaved");
                if (collected != null) {
                    totalCollected += collected;
                }
                
                log.info("✅ LOST112 데이터 수집 완료: {}건", collected);
            } else {
                result.put("lost112", Map.of("message", "수집 건너뜀"));
                log.info("⏭️  LOST112 데이터 수집 건너뜀");
            }

            // 2. 서울교통공사 데이터 수집
            if (Boolean.TRUE.equals(request.getCollectSeoul())) {
                log.info("📥 서울교통공사 데이터 수집 시작...");
                Map<String, Object> seoulResult = collectSeoulData();
                result.put("seoul", seoulResult);
                
                Integer collected = (Integer) seoulResult.get("imported");
                if (collected != null) {
                    totalCollected += collected;
                }
                
                log.info("✅ 서울교통공사 데이터 수집 완료: {}건", collected);
            } else {
                result.put("seoul", Map.of("message", "수집 건너뜀"));
                log.info("⏭️  서울교통공사 데이터 수집 건너뜀");
            }

            // 3. 깨진 데이터 자동 정리
            if (Boolean.TRUE.equals(request.getAutoCleanup())) {
                log.info("🧹 깨진 데이터 자동 정리 시작...");
                int cleaned = dataCleanupService.cleanupBrokenData();
                totalBrokenCleaned = cleaned;
                result.put("cleanup", Map.of(
                    "cleaned", cleaned,
                    "message", cleaned > 0 
                        ? "깨진 데이터 " + cleaned + "건 삭제 완료" 
                        : "깨진 데이터 없음"
                ));
                log.info("✅ 깨진 데이터 정리 완료: {}건 삭제", cleaned);
            } else {
                result.put("cleanup", Map.of("message", "정리 건너뜀"));
                log.info("⏭️  깨진 데이터 정리 건너뜀");
            }

            // 4. 최종 통계
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            result.put("success", true);
            result.put("totalCollected", totalCollected);
            result.put("totalBrokenCleaned", totalBrokenCleaned);
            result.put("netGain", totalCollected - totalBrokenCleaned);
            result.put("endTime", endTime.format(FORMATTER));
            result.put("durationSeconds", durationSeconds);
            result.put("message", String.format(
                "통합 수집 완료! 총 %d건 수집, %d건 정리 → 순증가 %d건 (%d초 소요)",
                totalCollected, totalBrokenCleaned, totalCollected - totalBrokenCleaned, durationSeconds
            ));

            log.info("========================================");
            log.info("통합 데이터 수집 완료!");
            log.info("총 수집: {}건, 정리: {}건, 순증가: {}건", 
                    totalCollected, totalBrokenCleaned, totalCollected - totalBrokenCleaned);
            log.info("소요 시간: {}초", durationSeconds);
            log.info("========================================");

            return result;

        } catch (Exception e) {
            log.error("통합 데이터 수집 중 오류 발생", e);
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("totalCollected", totalCollected);
            result.put("totalBrokenCleaned", totalBrokenCleaned);
            result.put("endTime", endTime.format(FORMATTER));
            result.put("durationSeconds", durationSeconds);

            return result;
        }
    }

    /**
     * LOST112 데이터 수집
     */
    private Map<String, Object> collectLost112Data(IntegratedImportRequest request) {
        try {
            return lost112ImportService.importData(
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getRegionCode(),
                    request.getMaxPages(),
                    request.getRowsPerPage()
            );
        } catch (Exception e) {
            log.error("LOST112 데이터 수집 실패", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "newlySaved", 0
            );
        }
    }

    /**
     * 서울교통공사 데이터 수집
     */
    private Map<String, Object> collectSeoulData() {
        try {
            int imported = seoulLostService.importSeoulLostItems();
            return Map.of(
                    "success", true,
                    "imported", imported,
                    "message", "서울교통공사 데이터 " + imported + "건 수집 완료"
            );
        } catch (Exception e) {
            log.error("서울교통공사 데이터 수집 실패", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "imported", 0
            );
        }
    }

    /**
     * 데이터베이스 현재 상태를 조회합니다.
     * 
     * @return 전체 데이터 개수, 소스별 개수, 최근 데이터 등
     */
    public Map<String, Object> getDataStatus() {
        return dataCleanupService.getDataStatus();
    }

    /**
     * 깨진 데이터를 즉시 정리합니다.
     * 
     * @return 정리된 데이터 개수
     */
    public Map<String, Object> cleanupNow() {
        log.info("🧹 즉시 정리 요청 - 깨진 데이터 검색 및 삭제 시작");
        
        int estimatedCount = dataCleanupService.estimateBrokenDataCount();
        log.info("깨진 데이터 예상 개수: {}건", estimatedCount);
        
        int cleaned = dataCleanupService.cleanupBrokenData();
        
        return Map.of(
                "success", true,
                "estimatedCount", estimatedCount,
                "actualCleaned", cleaned,
                "message", cleaned > 0 
                    ? "깨진 데이터 " + cleaned + "건 삭제 완료" 
                    : "깨진 데이터가 없습니다"
        );
    }
}

