package com.lostfound.capstonebackend.domain.lost112.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * LOST112 API의 전체 응답 구조를 나타내는 최상위 DTO입니다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lost112ResponseDto {

    /**
     * API 응답의 루트 요소
     */
    @JsonProperty("response")
    private Response response;

    /**
     * API 응답의 'response' 필드를 나타냅니다. 헤더와 바디를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {

        /**
         * 응답 헤더
         */
        @JsonProperty("header")
        private Header header;

        /**
         * 응답 본문
         */
        @JsonProperty("body")
        private Body body;
    }

    /**
     * API 응답의 'header' 필드를 나타냅니다. API 호출의 결과 코드를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {

        /**
         * API 처리 결과 코드 (예: "00"은 성공)
         */
        @JsonProperty("resultCode")
        private String resultCode;

        /**
         * API 처리 결과 메시지
         */
        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    /**
     * API 응답의 'body' 필드를 나타냅니다. 실제 데이터와 페이지네이션 정보를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {

        /**
         * 분실물 데이터 목록을 감싸는 컨테이너
         */
        @JsonProperty("items")
        private Items items;

        /**
         * 한 페이지에 표시되는 항목의 수
         */
        @JsonProperty("numOfRows")
        private Integer numOfRows;

        /**
         * 현재 페이지 번호
         */
        @JsonProperty("pageNo")
        private Integer pageNo;

        /**
         * 전체 검색 결과의 수
         */
        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    /**
     * 분실물 데이터 목록을 감싸는 'items' 필드를 나타냅니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {

        /**
         * 실제 분실물 데이터 DTO({@link Lost112ItemDto})의 리스트
         */
        @JsonProperty("item")
        private List<Lost112ItemDto> item;
    }
}