package com.lostfound.capstonebackend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션에서 발생하는 모든 비즈니스 관련 에러 코드를 정의하는 열거형입니다.
 * 각 에러 코드는 HTTP 상태, 고유 코드, 기본 메시지를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== 공통 에러 (Common Errors) ==========
    /** 요청 DTO의 필드 유효성 검사(예: @Valid)에 실패한 경우 */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    /** 데이터베이스에서 특정 엔티티를 찾을 수 없는 경우 */
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    /** 생성하려는 리소스가 이미 존재하는 경우 (예: 중복된 데이터) */
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    /** 서버 내부 로직 처리 중 발생한 예측하지 못한 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버에 오류가 발생했습니다."),

    // ========== 사용자 관련 에러 (User Errors) ==========
    /** 회원가입 시 이미 사용 중인 이메일로 가입을 시도하는 경우 */
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    /** 회원가입 시 이미 사용 중인 아이디(username)로 가입을 시도하는 경우 */
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "이미 사용 중인 아이디입니다."),
    /** 로그인 시 이메일 또는 비밀번호가 일치하지 않는 경우 */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** 특정 사용자를 찾을 수 없는 경우 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // ========== 인증/인가 관련 에러 (Authentication & Authorization Errors) ==========
    /** JWT 토큰의 형식이 잘못되었거나 서명이 유효하지 않은 경우 */
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_JWT_TOKEN", "유효하지 않은 토큰입니다."),
    /** JWT 토큰이 만료된 경우 */
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_JWT_TOKEN", "만료된 토큰입니다."),
    /** 인증이 필요한 요청에 JWT 토큰이 포함되지 않은 경우 */
    JWT_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "JWT_TOKEN_REQUIRED", "인증 토큰이 필요합니다."),
    /** 인증은 되었으나 해당 리소스에 접근할 권한이 없는 경우 */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),
    /** 인증되지 않은 사용자가 보호된 리소스에 접근을 시도하는 경우 */
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다.");

    /** HTTP 응답 상태 코드 */
    private final HttpStatus status;
    /** API 응답에 포함될 고유 에러 코드 문자열 */
    private final String code;
    /** 에러에 대한 기본 설명 메시지 */
    private final String defaultMessage;
}
