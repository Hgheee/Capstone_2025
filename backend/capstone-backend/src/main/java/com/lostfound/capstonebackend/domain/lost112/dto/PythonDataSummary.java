package com.lostfound.capstonebackend.domain.lost112.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 임시 테이블에 저장된 Python 수집 데이터의 요약 정보를 담는 DTO입니다.
 * 페이지네이션 정보와 현재 페이지의 데이터 목록을 포함합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonDataSummary {

    /**
     * 임시 테이블에 있는 전체 데이터의 수
     */
    private long totalCount;

    /**
     * 현재 페이지에 포함된 데이터의 수
     */
    private int currentPageCount;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int pageNumber;

    /**
     * 한 페이지의 크기
     */
    private int pageSize;

    /**
     * 현재 페이지에 해당하는 분실물 데이터 목록
     */
    private List<Lost112ItemDto> items;
}
