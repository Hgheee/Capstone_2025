package com.lostfound.capstonebackend.common.dto;

/**
 * 모든 API 응답을 위한 표준 형식의 제네릭 레코드입니다.
 *
 * @param <T>     응답 데이터의 타입
 * @param success API 호출의 성공 여부
 * @param data    성공 시 반환될 데이터 (실패 시 null)
 * @param error   실패 시 반환될 에러 정보 (성공 시 null)
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /**
     * 데이터가 포함된 성공 응답을 생성합니다.
     *
     * @param data 응답에 포함될 데이터
     * @param <T>  데이터의 타입
     * @return 데이터가 포함된 ApiResponse 객체
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 데이터가 없는 성공 응답을 생성합니다. (예: 삭제 성공)
     *
     * @return 데이터가 없는 ApiResponse 객체
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 상세 정보가 없는 실패 응답을 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @return 에러 정보가 포함된 ApiResponse 객체
     */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, ApiError.of(code, message));
    }

    /**
     * 상세 정보가 포함된 실패 응답을 생성합니다.
     *
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param details 추가적인 에러 상세 정보
     * @return 에러 정보와 상세 내용이 포함된 ApiResponse 객체
     */
    public static ApiResponse<Void> fail(String code, String message, java.util.Map<String, Object> details) {
        return new ApiResponse<>(false, null, ApiError.of(code, message, details));
    }
}
