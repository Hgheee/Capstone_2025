package com.lostfound.capstonebackend.common.util;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET = "test-secret-key-for-jwt-signing-must-be-long-enough-1234567890";

    @Test
    @DisplayName("JWT 토큰 생성 및 검증")
    void generateAndValidateToken() {
        // Given
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMinutes(15).toMillis());

        // When
        String token = jwtUtils.generateToken("user@example.com");

        // Then
        assertThat(token).isNotBlank();
        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtUtils.getJtiFromToken(token)).isNotBlank();
        Date expiration = jwtUtils.getExpirationDateFromToken(token);
        assertThat(expiration).isAfter(new Date());
        assertThat(jwtUtils.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증")
    void validateExpiredToken() {
        // Given: 만료 시간을 과거로 설정하여 즉시 만료된 토큰 생성
        JwtUtils jwtUtils = new JwtUtils(SECRET, -1000L);
        String token = jwtUtils.generateToken("expired@example.com");

        // When & Then
        assertThat(jwtUtils.validateToken(token)).isFalse();
        assertThat(jwtUtils.isTokenExpired(token)).isTrue();
        assertThatThrownBy(() -> jwtUtils.getEmailFromToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
