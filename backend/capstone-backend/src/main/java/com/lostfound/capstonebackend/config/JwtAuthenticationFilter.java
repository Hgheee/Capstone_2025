package com.lostfound.capstonebackend.config;

import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Spring Security 필터 체인에서 JWT(JSON Web Token)를 이용한 인증을 처리하는 필터입니다.
 * 매 HTTP 요청마다 한 번씩 실행되며, 요청 헤더의 JWT를 검증하여 유효한 경우
 * 해당 요청에 대한 사용자 인증 정보를 SecurityContext에 설정합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils; // JWT 토큰 생성, 검증, 정보 추출 유틸
    private final UserRepository userRepository; // DB에서 사용자 정보 조회
    private final BlacklistedTokenRepository blacklistedTokenRepository; // 로그아웃 처리된 토큰(블랙리스트) 조회

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 실제 필터링 로직을 수행하는 메소드입니다.
     * 1. 요청에서 JWT 토큰을 추출합니다.
     * 2. 토큰의 유효성을 검증하고, 블랙리스트에 포함되어 있는지 확인합니다.
     * 3. 유효한 토큰인 경우, 토큰에서 사용자 이메일을 추출하여 DB에서 사용자 정보를 조회합니다.
     * 4. 조회된 사용자 정보를 기반으로 Spring Security의 인증 객체(Authentication)를 생성합니다.
     * 5. 생성된 인증 객체를 SecurityContextHolder에 설정하여 현재 요청을 인증된 상태로 만듭니다.
     *
     * @param request  HTTP 서블릿 요청 객체
     * @param response HTTP 서블릿 응답 객체
     * @param filterChain 필터 체인 객체
     * @throws ServletException 서블릿 처리 중 발생하는 예외
     * @throws IOException 입출력 처리 중 발생하는 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            String jwt = parseJwt(request);

            // 2. 토큰이 존재하고 유효한 경우 인증 처리
            if (jwt != null && jwtUtils.validateToken(jwt)) {
                // 2.1. 로그아웃된 토큰(블랙리스트)인지 확인
                try {
                    String jti = jwtUtils.getJtiFromToken(jwt);
                    if (jti != null && blacklistedTokenRepository.existsByToken(jti)) {
                        log.warn("거부된 토큰입니다. (블랙리스트) JTI: {}", jti);
                        SecurityContextHolder.clearContext(); // 보안 컨텍스트 정리
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (Exception e) {
                    log.warn("JTI 추출 실패, 블랙리스트 검사 없이 진행: {}", e.getMessage());
                }

                // 3. 토큰에서 사용자 이메일(subject) 추출
                String email = jwtUtils.getEmailFromToken(jwt);

                // 4. DB에서 사용자 정보 조회
                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // 5. Spring Security용 인증 객체 생성
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(user.getRole().getKey()));

                    UserDetails authenticatedUser = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                            .password("") // 비밀번호는 인증 완료 후 필요 없으므로 비워둠
                            .authorities(authorities)
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    authenticatedUser, // Principal (인증된 사용자 정보)
                                    null,              // Credentials (비밀번호, 필요 없음)
                                    authorities        // Authorities (권한 목록)
                            );

                    // 요청 상세 정보 설정
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 성공. 사용자: {}, 권한: {}", email, authorities);
                } else {
                    log.warn("토큰은 유효하지만 사용자를 찾을 수 없습니다. 이메일: {}", email);
                    SecurityContextHolder.clearContext(); // 사용자가 없으므로 컨텍스트 정리
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 필터에서 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext(); // 예외 발생 시 컨텍스트를 안전하게 정리
        }

        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청의 'Authorization' 헤더에서 'Bearer ' 접두사를 제거하고 순수한 JWT 토큰 문자열을 추출합니다.
     *
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열. 헤더가 없거나 형식이 올바르지 않으면 null을 반환합니다.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 특정 경로의 요청에 대해 이 필터의 실행을 건너뛸지 여부를 결정합니다.
     * 로그인, 회원가입, Swagger UI, Health Check 등 인증이 필요 없는 공개 엔드포인트 경로를 정의합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 필터를 적용하지 않으려면 true, 적용하려면 false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 아래 경로들은 JWT 인증 필터를 거치지 않음 (공개 API)
        return path.equals("/") ||
                path.startsWith("/api/health") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/signup") ||
                path.startsWith("/api/lost-items") ||  // ✅ 분실물 조회 API 추가
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/static/") ||
                path.startsWith("/error");
    }
}
