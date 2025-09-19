package com.lostfound.capstonebackend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lostfound.capstonebackend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 * 클라이언트에게 사용자 정보를 전달할 때 사용됩니다.
 * 보안상 비밀번호는 제외됩니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Builder
@Schema(description = "사용자 정보 응답")
public class UserResponse {

    /**
     * 사용자 고유 식별자
     */
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    /**
     * 사용자 이메일 주소
     */
    @Schema(description = "사용자 이메일 주소", example = "user@example.com")
    private String email;

    /**
     * 사용자 이름
     */
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    /**
     * 사용자 전화번호
     */
    @Schema(description = "사용자 전화번호", example = "010-1234-5678")
    private String phone;

    /**
     * 사용자 계정 생성 일시
     */
    @Schema(description = "계정 생성 일시", example = "2025-09-19T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 사용자 정보 수정 일시
     */
    @Schema(description = "정보 수정 일시", example = "2025-09-19T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * User 엔티티로부터 UserResponse 생성하는 정적 팩토리 메소드
     *
     * @param user User 엔티티
     * @return UserResponse 객체
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}