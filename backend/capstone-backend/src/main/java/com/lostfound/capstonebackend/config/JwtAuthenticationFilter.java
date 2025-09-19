package com.lostfound.capstonebackend.config;

import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JWT 인증 필터
 * - 매 요청마다 실행됨 (OncePerRequestFilter)
 * - Authorization 헤더에서 JWT 추출 후 검증
 * - 유효하면 SecurityContext에 인증 정보 저장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;              // JWT 토큰 유틸
    private final UserRepository userRepository;  // DB에서 사용자 조회
    private final com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository blacklistedTokenRepository;  // DB에서 사용자 조회

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 실제 필터 로직
     * 1. Authorization 헤더에서 JWT 추출
     * 2. 토큰 검증
     * 3. 토큰에서 이메일 추출 후 DB 조회
     * 4. 인증 정보(SecurityContext)에 저장
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // JWT 토큰 추출
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateToken(jwt)) {
                try {
                    String jti = jwtUtils.getJtiFromToken(jwt);
                    if (jti != null && blacklistedTokenRepository.existsByToken(jti)) {
                        log.warn("Blacklisted JWT token rejected");
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (Exception ignore) {}

                // 토큰에서 이메일(subject) 추출
                String email = jwtUtils.getEmailFromToken(jwt);

                // DB에서 사용자 조회
                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // 권한(Role) 부여 (기본 USER)
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_USER")
                    );

                    // 인증 객체 생성 (Principal = 이메일)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null, // 비밀번호 필요 없음
                                    authorities
                            );

                    // 요청 정보 세팅
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext에 인증 저장
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT authentication successful for user: {}", email);
                } else {
                    log.warn("User not found for email: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
            SecurityContextHolder.clearContext(); // 인증 실패 시 컨텍스트 비움
        }

        // 다음 필터 실행
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (없으면 null)
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 특정 요청에 대해 JWT 인증 필터를 적용하지 않음
     * - 로그인, 회원가입, health 체크 등은 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/") ||
                path.startsWith("/api/health") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/signup") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/static/") ||
                path.startsWith("/error");
    }
}






