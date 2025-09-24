package com.lostfound.capstonebackend.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 상의 예외를 표현하기 위한 커스텀 런타임 예외 클래스입니다.
 * 이 예외는 {@link GlobalExceptionHandler}에 의해 처리되어 일관된 API 에러 응답으로 변환됩니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 예외에 해당하는 에러 코드 (ErrorCode 열거형)
     */
    private final ErrorCode errorCode;

    /**
     * 주어진 ErrorCode를 사용하여 BusinessException을 생성합니다.
     * 예외 메시지는 ErrorCode의 기본 메시지를 사용합니다.
     * @param errorCode 발생한 에러의 종류를 정의하는 ErrorCode
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 주어진 ErrorCode와 커스텀 메시지를 사용하여 BusinessException을 생성합니다.
     * @param errorCode 발생한 에러의 종류를 정의하는 ErrorCode
     * @param message 기본 메시지를 오버라이드하는 상세 예외 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
