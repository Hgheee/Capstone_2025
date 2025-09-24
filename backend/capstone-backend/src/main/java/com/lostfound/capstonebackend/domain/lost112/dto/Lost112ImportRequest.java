package com.lostfound.capstonebackend.domain.lost112.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * LOST112 데이터 수집을 요청할 때 사용되는 파라미터를 담는 DTO입니다.
 * 사용자는 이 DTO를 통해 수집할 데이터의 기간, 지역, 규모 등을 지정할 수 있습니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lost112ImportRequest {

    /**
     * 데이터 수집 시작일 (yyyy-MM-dd 형식)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 데이터 수집 종료일 (yyyy-MM-dd 형식)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 수집할 지역 코드 (예: "SEOUL", "BUSAN")
     */
    private String regionCode;

    /**
     * 수집할 최대 페이지 수
     */
    private Integer maxPages;

    /**
     * 한 페이지당 수집할 데이터 행의 수
     */
    private Integer rowsPerPage;
}
