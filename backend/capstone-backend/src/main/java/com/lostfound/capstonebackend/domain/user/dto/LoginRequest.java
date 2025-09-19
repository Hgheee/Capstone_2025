package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 요청 DTO
 * 사용자 로그인 시 클라이언트로부터 받는 인증 정보를 담습니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "로그인 요청 정보")
public class LoginRequest {

    /**
     * 사용자 이메일 주소 (로그인 ID)
     */
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    private String email;

    /**
     * 사용자 비밀번호
     */
    @Schema(description = "사용자 비밀번호", example = "password123!", required = true)
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 1, max = 50, message = "비밀번호를 입력해주세요.")
    private String password;

    /**
     * 생성자
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}