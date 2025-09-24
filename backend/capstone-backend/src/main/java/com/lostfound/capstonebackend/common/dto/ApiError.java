package com.lostfound.capstonebackend.common.dto;

import java.util.Map;

/**
 * API 에러 응답의 상세 내용을 표현하는 레코드(Record)입니다.
 *
 * @param code    에러를 식별하는 고유 코드 (예: "INVALID_INPUT_VALUE")
 * @param message 에러에 대한 사용자 친화적 설명
 * @param details 에러에 대한 추가적인 상세 정보 (선택 사항)
 */
public record ApiError(String code, String message, Map<String, Object> details) {

    /**
     * 상세 정보(details) 없이 ApiError 객체를 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @return 새로운 ApiError 인스턴스
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    /**
     * 모든 필드를 사용하여 ApiError 객체를 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param details 추가 상세 정보 맵
     * @return 새로운 ApiError 인스턴스
     */
    public static ApiError of(String code, String message, Map<String, Object> details) {
        return new ApiError(code, message, details);
    }
}
