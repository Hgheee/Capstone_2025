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

    /**
     * 수정할 사용자 이름
     */
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
    private String name;

    /**
     * 수정할 사용자 전화번호 (선택사항)
     */
    @Schema(description = "사용자 전화번호", example = "010-1234-5678", required = false)
    @Pattern(
        regexp = "^(010|011|016|017|018|019)-\\d{3,4}-\\d{4}$|^$",
        message = "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)"
    )
    private String phone;

    /**
     * 생성자
     *
     * @param name  사용자 이름
     * @param phone 사용자 전화번호
     */
    public UserUpdateRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}