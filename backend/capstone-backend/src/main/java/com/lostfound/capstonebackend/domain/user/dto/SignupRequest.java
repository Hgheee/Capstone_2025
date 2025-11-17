package com.lostfound.capstonebackend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 요청 DTO
 * 새로운 사용자 등록 시 클라이언트로부터 받는 데이터를 담습니다.
 *
 * @author Capstone Team
 * @version 1.1
 * @since 2025-09-19
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "회원가입 요청 정보")
public class SignupRequest {

    /** 사용자 이메일 주소 */
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    private String email;

    /** 로그인 아이디(username) - 이메일과 동일하게 자동 설정됨 */
    @Schema(description = "로그인 아이디(이메일과 동일)", example = "user@example.com", required = false)
    private String username;

    /** 사용자 비밀번호 */
    @Schema(description = "사용자 비밀번호(영문/숫자/특수문자 포함 8~50자)", example = "password123!", required = true)
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하로 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String password;

    /** 사용자 이름 */
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
    private String name;

    /** 사용자 전화번호 (선택사항) */
    @Schema(description = "사용자 전화번호(하이픈 유무 모두 허용)", example = "010-1234-5678", required = false)
    @Pattern(
        // 010/011/016/017/018/019 + (하이픈 선택) 3~4자리 + (하이픈 선택) 4자리, 혹은 빈 값 허용
        regexp = "^(?:010|011|016|017|018|019)(?:-?\\d{3,4})(?:-?\\d{4})$|^$",
        message = "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678 또는 01012345678)"
    )
    private String phone;

    /**
     * User 엔티티로 변환하는 메소드
     * 비밀번호는 암호화되지 않은 상태로 반환됩니다. (Service에서 암호화)
     * username이 없으면 email을 username으로 사용합니다.
     */
    public com.lostfound.capstonebackend.domain.user.User toEntity() {
        String finalUsername = (username != null && !username.isBlank()) ? username : email;
        return com.lostfound.capstonebackend.domain.user.User.builder()
                .username(finalUsername)   // ✅ username이 없으면 email을 사용
                .email(email)
                .password(password)   // 암호화는 Service에서 처리
                .name(name)
                .phone(phone)
                .build();
    }
}
