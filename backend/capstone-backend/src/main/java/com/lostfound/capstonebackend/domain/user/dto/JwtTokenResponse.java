package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * JWT 토큰 응답 DTO
 * 로그인 성공 시 클라이언트에게 JWT 토큰과 사용자 정보를 전달합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Builder
@Schema(description = "JWT 토큰 응답 정보")
public class JwtTokenResponse {

    /**
     * JWT Access Token
     */
    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    /**
     * 토큰 타입 (Bearer)
     */
    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    /**
     * 토큰 만료 시간 (초 단위)
     */
    @Schema(description = "토큰 만료 시간 (초)", example = "86400")
    private Long expiresIn;

    /**
     * 토큰 발급 시간
     */
    @Schema(description = "토큰 발급 시간", example = "2025-09-19T10:30:00")
    private LocalDateTime issuedAt;

    /**
     * 사용자 정보
     */
    @Schema(description = "로그인된 사용자 정보")
    private UserResponse user;

    /**
     * JWT 토큰 응답 생성하는 정적 팩토리 메소드
     *
     * @param accessToken JWT Access Token
     * @param expiresIn   토큰 만료 시간 (초)
     * @param user        사용자 정보
     * @return JwtTokenResponse 객체
     */
    public static JwtTokenResponse of(String accessToken, Long expiresIn, UserResponse user) {
        return JwtTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .issuedAt(LocalDateTime.now())
                .user(user)
                .build();
    }
}