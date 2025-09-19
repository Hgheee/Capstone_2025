package com.lostfound.capstonebackend.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 * JWT Access Token의 생성, 검증, 파싱 기능을 제공합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Component
@Slf4j
public class JwtUtils {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    /**
     * JwtUtils 생성자
     * 환경변수에서 JWT 설정값을 읽어와 초기화합니다.
     *
     * @param jwtSecret     JWT 서명에 사용할 비밀키
     * @param jwtExpiration JWT 토큰 만료 시간 (밀리초)
     */
    public JwtUtils(@Value("${JWT_SECRET_KEY}") String jwtSecret,
                    @Value("${JWT_EXPIRATION_TIME:900000}") long jwtExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpiration;
        log.info("JwtUtils initialized with expiration: {} ms", jwtExpirationMs);
    }

    /**
     * 사용자 이메일을 기반으로 JWT 토큰을 생성합니다.
     *
     * @param email 사용자 이메일 (JWT subject로 사용)
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(email)
                .setId(java.util.UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("JWT token generated for user: {}", email);
        return token;
    }

    /**
     * JWT 토큰에서 사용자 이메일을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 이메일
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException e) {
            log.error("Failed to extract email from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰에서 JTI(토큰 ID)를 추출합니다.
     * @param token JWT 토큰
     * @return JTI 값
     * @throws JwtException 토큰 유효성 문제 시
     */
    public String getJtiFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId();
        } catch (JwtException e) {
            log.error("Failed to extract jti from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰의 유효성을 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return true: 유효한 토큰, false: 유효하지 않은 토큰
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            log.debug("JWT token validation successful");
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰의 만료 시간을 반환합니다.
     *
     * @param token JWT 토큰
     * @return 만료 시간 (Date 객체)
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration();
        } catch (JwtException e) {
            log.error("Failed to extract expiration date from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰의 만료 여부를 확인합니다.
     *
     * @param token JWT 토큰
     * @return true: 만료된 토큰, false: 유효한 토큰
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true; // 토큰이 유효하지 않으면 만료된 것으로 간주
        }
    }

    /**
     * JWT 토큰 만료 시간을 초 단위로 반환합니다.
     *
     * @return JWT 토큰 만료 시간 (초)
     */
    public long getExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }
}
