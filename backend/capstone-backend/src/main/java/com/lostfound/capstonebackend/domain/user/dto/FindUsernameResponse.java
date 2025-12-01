package com.lostfound.capstonebackend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이디 찾기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FindUsernameResponse {
    private String username;
    private String email;
    
    public static FindUsernameResponse of(String username, String email) {
        return new FindUsernameResponse(username, email);
    }
}

