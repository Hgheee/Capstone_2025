package com.lostfound.capstonebackend.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT(JSON Web Token)의 생성, 검증, 정보 추출을 담당하는 유틸리티 클래스입니다.
 */
@Component
@Slf4j
public class JwtUtils {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    /**
     * JwtUtils 생성자.
     * application.yml 또는 환경변수에서 JWT 설정값을 읽어와 초기화합니다.
     *
     * @param jwtSecret     HMAC-SHA 알고리즘에 사용할 Base64 인코딩된 비밀키
     * @param jwtExpiration JWT 토큰 만료 시간 (밀리초 단위)
     */
    public JwtUtils(@Value("${jwt.secret-key}") String jwtSecret,
                    @Value("${jwt.access-token.expiration:900000}") long jwtExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpiration;
        log.info("JwtUtils initialized with expiration: {} ms", jwtExpirationMs);
    }

    /**
     * 사용자 이메일을 기반으로 새로운 JWT 액세스 토큰을 생성합니다.
     * - subject: 사용자 이메일
     * - jti: 토큰 고유 ID (로그아웃 시 블랙리스트에 사용)
     * - issuedAt: 발급 시간
     * - expiration: 만료 시간
     *
     * @param email 사용자의 이메일 주소 (토큰의 subject 클레임)
     * @return 생성된 JWT 문자열
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(email)
                .setId(UUID.randomUUID().toString()) // 토큰 ID (JTI) 설정
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("JWT token generated for user: {}", email);
        return token;
    }

    /**
     * JWT 토큰을 파싱하여 사용자 이메일(subject)을 추출합니다.
     *
     * @param token 파싱할 JWT 토큰
     * @return 추출된 사용자 이메일
     * @throws JwtException 토큰이 유효하지 않거나 파싱에 실패한 경우
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
            log.error("JWT 토큰에서 이메일 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰에서 JTI(JWT ID) 클레임을 추출합니다.
     * 이 값은 토큰을 고유하게 식별하며, 로그아웃 시 토큰을 블랙리스트에 추가하는 데 사용됩니다.
     *
     * @param token 파싱할 JWT 토큰
     * @return 추출된 JTI 값
     * @throws JwtException 토큰이 유효하지 않거나 파싱에 실패한 경우
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
            log.error("JWT 토큰에서 JTI 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 주어진 JWT 토큰의 유효성을 검증합니다.
     * 서명, 만료 시간, 형식 등을 종합적으로 확인합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            log.debug("JWT 토큰 검증 성공");
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명 또는 형식입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 만료 시간을 추출합니다.
     *
     * @param token 파싱할 JWT 토큰
     * @return 토큰의 만료 시간(Date 객체)
     * @throws JwtException 토큰이 유효하지 않거나 파싱에 실패한 경우
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
            log.error("JWT 토큰에서 만료 날짜 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰이 만료되었는지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 토큰이 만료되었거나 유효하지 않으면 true, 그렇지 않으면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            // 파싱 자체가 실패하는 토큰(예: 형식 오류, 서명 오류)은 만료된 것으로 간주하여 안전하게 처리합니다.
            return true;
        }
    }

    /**
     * 설정된 토큰 만료 시간을 초 단위로 반환합니다.
     *
     * @return 토큰 만료 시간 (초)
     */
    public long getExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }
}
