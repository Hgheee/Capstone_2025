package com.lostfound.capstonebackend.domain.lost112.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LOST112 API 응답에서 개별 분실물 항목을 표현하는 DTO입니다.
 * {@link JsonProperty} 애노테이션을 사용하여 API의 필드명을 내부 필드명에 매핑합니다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lost112ItemDto {

    /**
     * 관리ID (LOST112 시스템의 고유 식별자)
     */
    @JsonProperty("atcId")
    private String atcId;

    /**
     * 물품명
     */
    @JsonProperty("fdPrdtNm")
    private String fdPrdtNm;

    /**
     * 습득물 상세 내용
     */
    @JsonProperty("fdSbjt")
    private String fdSbjt;

    /**
     * 습득일자 (YYYYMMDD 형식의 문자열)
     */
    @JsonProperty("fdYmd")
    private String fdYmd;

    /**
     * 물품 분류명 (예: 가방, 지갑, 휴대폰)
     */
    @JsonProperty("prdtClNm")
    private String prdtClNm;

    /**
     * 색상명
     */
    @JsonProperty("clrNm")
    private String clrNm;

    /**
     * 보관 장소 (예: OOO 경찰서)
     */
    @JsonProperty("depPlace")
    private String depPlace;

    /**
     * 분실물 이미지 파일의 URL 경로
     */
    @JsonProperty("fdFilePathImg")
    private String fdFilePathImg;
}