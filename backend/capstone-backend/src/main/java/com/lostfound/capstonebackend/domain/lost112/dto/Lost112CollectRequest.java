package com.lostfound.capstonebackend.domain.lost112.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * LOST112 데이터 수집 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lost112CollectRequest {

    /**
     * 수집 시작일 (yyyy-MM-dd)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 수집 종료일 (yyyy-MM-dd)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 지역 코드 (null 또는 공백이면 전체)
     */
    private String regionCode;
}
