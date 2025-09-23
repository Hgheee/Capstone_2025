package com.lostfound.capstonebackend.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자의 권한 등급을 정의하는 열거형(Enum)입니다.
 * Spring Security에서 사용자의 역할을 식별하는 데 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    /**
     * 일반 사용자 권한입니다.
     * 대부분의 사용자가 이 권한을 가집니다.
     */
    USER("ROLE_USER", "일반사용자"),

    /**
     * 관리자 권한입니다.
     * 데이터 관리 등 시스템의 모든 기능에 접근할 수 있습니다.
     */
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String description;
}
