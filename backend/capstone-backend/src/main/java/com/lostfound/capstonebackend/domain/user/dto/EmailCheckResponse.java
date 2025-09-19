package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 이메일 중복 검사 응답 DTO
 * 이메일 사용 가능 여부를 클라이언트에게 전달합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Builder
@Schema(description = "이메일 중복 검사 응답")
public class EmailCheckResponse {

    /**
     * 이메일 사용 가능 여부
     */
    @Schema(description = "이메일 사용 가능 여부", example = "true")
    private boolean available;

    /**
     * 결과 메시지
     */
    @Schema(description = "결과 메시지", example = "사용 가능한 이메일입니다.")
    private String message;

    /**
     * 이메일 중복 검사 결과 응답 생성하는 정적 팩토리 메소드
     *
     * @param available 이메일 사용 가능 여부
     * @return EmailCheckResponse 객체
     */
    public static EmailCheckResponse of(boolean available) {
        String message = available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.";
        
        return EmailCheckResponse.builder()
                .available(available)
                .message(message)
                .build();
    }
}