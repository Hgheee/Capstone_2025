package com.lostfound.capstonebackend.common.dto;

public record ApiResponse<T>(boolean success, T data, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, ApiError.of(code, message));
    }
    public static ApiResponse<Void> fail(String code, String message, java.util.Map<String, Object> details) {
        return new ApiResponse<>(false, null, ApiError.of(code, message, details));
    }
}
