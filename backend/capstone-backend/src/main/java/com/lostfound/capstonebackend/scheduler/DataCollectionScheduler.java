package com.lostfound.capstonebackend.scheduler;

import com.lostfound.capstonebackend.domain.lost112.Lost112ApiService;
import com.lostfound.capstonebackend.domain.lost112.Lost112ImportService;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ImportRequest;
import com.lostfound.capstonebackend.domain.lostitem.LostItemService;
import com.lostfound.capstonebackend.domain.seoul.SeoulLostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 분실물 데이터를 주기적으로 자동 수집하는 스케줄러입니다.
 * application.yml에서 scheduler.enabled=true로 설정하면 활성화됩니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class DataCollectionScheduler {

    private final Lost112ApiService lost112ApiService;
    private final Lost112ImportService lost112ImportService;
    private final SeoulLostService seoulLostService;
    private final LostItemService lostItemService;

    /**
     * 매일 오전 3시에 LOST112 데이터를 자동으로 수집합니다.
     * 최근 7일치 데이터를 주요 지역(서울, 경기, 부산, 인천)에서 수집합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 오전 3시
    public void collectLost112DataDaily() {
        log.info("===== LOST112 자동 데이터 수집 시작 =====");
        
        try {
            String startYmd = LocalDate.now().minusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);
            String endYmd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            
            var regionCodes = Arrays.asList("11", "41", "26", "28"); // 서울, 경기, 부산, 인천
            
            var items = lost112ApiService.fetchAllRegions(regionCodes, startYmd, endYmd);
            log.info("LOST112 자동 수집 완료 - 총 {}건", items.size());
            
            // 지역 정보 업데이트
            lostItemService.updateAllRegions();
            log.info("지역 정보 업데이트 완료");
            
        } catch (Exception e) {
            log.error("LOST112 자동 수집 실패", e);
        }
        
        log.info("===== LOST112 자동 데이터 수집 종료 =====");
    }

    /**
     * 매일 오전 4시에 서울교통공사 데이터를 자동으로 수집합니다.
     */
    @Scheduled(cron = "0 0 4 * * *")  // 매일 오전 4시
    public void collectSeoulMetroDataDaily() {
        log.info("===== 서울교통공사 자동 데이터 수집 시작 =====");
        
        try {
            int count = seoulLostService.importSeoulLostItems();
            log.info("서울교통공사 자동 수집 완료 - 저장: {}건", count);
            
            // 지역 정보 업데이트
            lostItemService.updateAllRegions();
            log.info("지역 정보 업데이트 완료");
            
        } catch (Exception e) {
            log.error("서울교통공사 자동 수집 실패", e);
        }
        
        log.info("===== 서울교통공사 자동 데이터 수집 종료 =====");
    }

    /**
     * 매주 일요일 오전 5시에 전체 지역 정보를 재구축합니다.
     */
    @Scheduled(cron = "0 0 5 * * SUN")  // 매주 일요일 오전 5시
    public void rebuildRegionInfoWeekly() {
        log.info("===== 주간 지역 정보 재구축 시작 =====");
        
        try {
            lostItemService.updateAllRegions();
            
            long totalCount = lostItemService.getTotalCount();
            long withRegion = lostItemService.getCountWithRegion();
            double coverage = totalCount > 0 ? (withRegion * 100.0 / totalCount) : 0.0;
            
            log.info("지역 정보 재구축 완료 - 전체: {}건, 지역정보: {}건, 커버리지: {:.2f}%", 
                    totalCount, withRegion, coverage);
            
        } catch (Exception e) {
            log.error("지역 정보 재구축 실패", e);
        }
        
        log.info("===== 주간 지역 정보 재구축 종료 =====");
    }

    /**
     * 30분마다 통계를 로그에 출력합니다. (선택사항)
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)  // 30분마다
    public void logStatistics() {
        try {
            long totalCount = lostItemService.getTotalCount();
            long withRegion = lostItemService.getCountWithRegion();
            double coverage = totalCount > 0 ? (withRegion * 100.0 / totalCount) : 0.0;
            
            log.info("📊 데이터베이스 통계 - 전체: {}건, 지역정보: {}건 ({:.2f}%)", 
                    totalCount, withRegion, coverage);
            
        } catch (Exception e) {
            log.debug("통계 조회 실패", e);
        }
    }
}

