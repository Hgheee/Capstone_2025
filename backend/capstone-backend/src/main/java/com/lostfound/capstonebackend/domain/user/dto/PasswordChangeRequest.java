package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비밀번호 변경 요청 DTO
 * 기존 사용자의 비밀번호를 변경할 때 사용됩니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class PasswordChangeRequest {

    /**
     * 현재 비밀번호
     */
    @Schema(description = "현재 비밀번호", example = "currentPassword123!", required = true)
    @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
    private String currentPassword;

    /**
     * 새로운 비밀번호
     */
    @Schema(description = "새로운 비밀번호", example = "newPassword123!", required = true)
    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하로 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String newPassword;

    /**
     * 새로운 비밀번호 확인
     */
    @Schema(description = "새로운 비밀번호 확인", example = "newPassword123!", required = true)
    @NotBlank(message = "새 비밀번호 확인은 필수 입력값입니다.")
    private String confirmPassword;

    /**
     * 생성자
     *
     * @param currentPassword 현재 비밀번호
     * @param newPassword     새로운 비밀번호
     * @param confirmPassword 새로운 비밀번호 확인
     */
    public PasswordChangeRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    /**
     * 새 비밀번호와 확인 비밀번호가 일치하는지 검증합니다.
     *
     * @return true: 일치, false: 불일치
     */
    public boolean isPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}