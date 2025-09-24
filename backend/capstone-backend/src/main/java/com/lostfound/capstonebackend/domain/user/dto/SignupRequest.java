package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "회원가입 요청 정보")
public class SignupRequest {

    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    private String email;

    @Schema(description = "사용자 비밀번호", example = "Password123!", required = true)
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하로 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String password;

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
    private String name;

    @Schema(description = "사용자 닉네임", example = "hong123", required = true)
    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 3, max = 50, message = "닉네임은 3자 이상 50자 이하로 입력해주세요.")
    private String username;   // ✅ 추가됨

    @Schema(description = "사용자 전화번호", example = "010-1234-5678", required = false)
    @Pattern(
        regexp = "^(010|011|016|017|018|019)-\\d{3,4}-\\d{4}$|^$",
        message = "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)"
    )
    private String phone;

    public com.lostfound.capstonebackend.domain.user.User toEntity() {
        return com.lostfound.capstonebackend.domain.user.User.builder()
                .email(email)
                .password(password)
                .name(name)
                .username(username)  // ✅ 매핑
                .phone(phone)
                .build();
    }
}
