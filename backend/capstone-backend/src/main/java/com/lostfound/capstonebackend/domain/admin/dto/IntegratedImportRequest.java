package com.lostfound.capstonebackend.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 통합 데이터 수집 요청 DTO
 * LOST112와 서울교통공사 데이터를 동시에 수집할 때 사용합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedImportRequest {
    
    /**
     * 수집 시작 날짜 (YYYY-MM-DD)
     */
    private String startDate;
    
    /**
     * 수집 종료 날짜 (YYYY-MM-DD)
     */
    private String endDate;
    
    /**
     * LOST112 데이터 수집 여부
     */
    @Builder.Default
    private Boolean collectLost112 = true;
    
    /**
     * 서울교통공사 데이터 수집 여부
     */
    @Builder.Default
    private Boolean collectSeoul = true;
    
    /**
     * 수집 후 자동으로 깨진 데이터 정리 여부
     */
    @Builder.Default
    private Boolean autoCleanup = true;
    
    /**
     * LOST112 지역 코드 (예: "11" = 서울)
     */
    private String regionCode;
    
    /**
     * LOST112 최대 페이지 수
     */
    @Builder.Default
    private Integer maxPages = 10;
    
    /**
     * LOST112 페이지당 행 수
     */
    @Builder.Default
    private Integer rowsPerPage = 100;
}

