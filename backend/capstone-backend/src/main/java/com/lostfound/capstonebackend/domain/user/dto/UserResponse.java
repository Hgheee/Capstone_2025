package com.lostfound.capstonebackend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 클라이언트에 전달하기 위한 응답 DTO입니다.
 * API 응답 시 사용되며, 비밀번호와 같은 민감한 정보는 포함하지 않습니다.
 */
@Getter
@Builder
@Schema(description = "사용자 응답 정보")
public class UserResponse {

    /**
     * 사용자의 고유 식별자(ID)입니다.
     */
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    /**
     * 사용자의 이메일 주소입니다.
     */
    @Schema(description = "사용자 이메일 주소", example = "user@example.com")
    private String email;

    /**
     * 사용자의 이름입니다.
     */
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    /**
     * 사용자의 전화번호입니다.
     */
    @Schema(description = "사용자 전화번호", example = "010-1234-5678")
    private String phone;

    /**
     * 사용자의 권한 등급입니다. (예: USER, ADMIN)
     */
    @Schema(description = "사용자 권한", example = "USER")
    private UserRole role;

    /**
     * 사용자 계정이 생성된 일시입니다.
     */
    @Schema(description = "등록 일시", example = "2025-09-19T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 사용자 정보가 마지막으로 수정된 일시입니다.
     */
    @Schema(description = "수정 일시", example = "2025-09-19T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * User 엔티티 객체를 UserResponse DTO로 변환합니다.
     *
     * @param user 변환할 User 엔티티 객체
     * @return 변환된 UserResponse DTO 객체
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
