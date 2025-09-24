package com.lostfound.capstonebackend.domain.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 로그아웃 처리된 JWT를 저장하는 엔티티입니다. (토큰 블랙리스트)
 * 사용자가 로그아웃하면, 해당 토큰의 JTI(JWT ID)가 이 테이블에 저장됩니다.
 * {@link com.lostfound.capstonebackend.config.JwtAuthenticationFilter}에서 이 테이블을 조회하여
 * 한 번 로그아웃된 토큰이 재사용되는 것을 방지합니다.
 */
@Entity
@Table(name = "blacklisted_token", indexes = {
        @Index(name = "ux_blacklisted_token_token", columnList = "token", unique = true)
})
@Getter
@NoArgsConstructor
public class BlacklistedToken {

    /**
     * 블랙리스트 항목의 고유 식별자(PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 블랙리스트에 추가된 토큰의 고유 식별자(JTI). 전체 토큰 문자열이 아닙니다.
     */
    @Column(name = "token", nullable = false, length = 200, unique = true)
    private String token;

    /**
     * 해당 토큰이 만료되는 시간입니다. 이 시간이 지나면 DB에서 삭제해도 안전합니다.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * BlacklistedToken 엔티티의 생성자입니다.
     * @param token 블랙리스트에 추가할 토큰의 JTI
     * @param expiresAt 해당 토큰의 만료 시간
     */
    @Builder
    public BlacklistedToken(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
