package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 정보 수정 요청 DTO
 * 기존 사용자의 정보를 수정할 때 사용됩니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "사용자 정보 수정 요청")
public class UserUpdateRequest {

    @Schema(description = "로그인 아이디", example = "hong123", required = true)
    @NotBlank(message = "아이디는 필수 입력값입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    private String username;  // ✅ 추가

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    private String name;

    @Schema(description = "사용자 전화번호", example = "010-1234-5678", required = false)
    private String phone;

    public UserUpdateRequest(String username, String name, String phone) {
        this.username = username;
        this.name = name;
        this.phone = phone;
    }
}

