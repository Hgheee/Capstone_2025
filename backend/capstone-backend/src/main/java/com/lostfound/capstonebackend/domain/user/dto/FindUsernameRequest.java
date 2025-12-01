package com.lostfound.capstonebackend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이디 찾기 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FindUsernameRequest {
    
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;
    
    @NotBlank(message = "전화번호 또는 이메일을 입력해주세요.")
    private String phoneOrEmail;
}

