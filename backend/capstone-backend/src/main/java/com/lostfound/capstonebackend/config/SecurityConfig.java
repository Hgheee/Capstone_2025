package com.lostfound.capstonebackend.config;

import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * 애플리케이션의 보안 설정을 총괄하는 클래스입니다.
 * Spring Security를 사용하여 HTTP 요청에 대한 인증 및 인가 규칙을 정의합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
    );

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈을 등록합니다.
     * BCrypt 알고리즘(강도 12)을 사용합니다.
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Spring Security의 필터 체인을 구성합니다.
     * HTTP 요청에 대한 보안 규칙을 설정합니다.
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 토큰을 사용하는 REST API이므로, 상태를 저장하지 않는 CSRF 보호 기능은 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정을 적용합니다. (corsConfigurationSource 빈 사용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션을 사용하지 않는 Stateless 정책을 설정합니다. (JWT 기반 인증)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청에 대한 접근 권한을 설정합니다.
                .authorizeHttpRequests(auth -> {
                    boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
                    List<String> permitAll = new ArrayList<>(List.of(
                            "/",
                            "/api/health",
                            "/api/auth/login",
                            "/api/auth/signup",
                            "/api/lost-items/**",   // ✅ 분실물 조회 API는 공개
                            "/api/admin/region-stats",  // ✅ 지역 통계는 공개
                            "/api/admin/update-regions",  // ✅ 지역 업데이트는 공개 (임시)
                            "/api/admin/import/lost112-by-region",  // ✅ LOST112 지역별 수집 (임시)
                            "/favicon.ico",
                            "/error"
                    ));
                    // 개발(dev) 프로필일 경우 Swagger 관련 경로를 추가로 허용합니다.
                    if (isDev) {
                        permitAll.addAll(List.of(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ));
                    }

                    // 설정된 경로들은 인증 없이 접근 허용
                    auth.requestMatchers(permitAll.toArray(String[]::new)).permitAll()
                        // 그 외 모든 요청은 인증이 필요함
                        .anyRequest().authenticated();
                })

                // 직접 구현한 JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 위한 빈을 등록합니다.
     * 환경변수(ALLOWED_ORIGINS)에서 허용할 출처를 읽어와 동적으로 설정합니다.
     * 환경변수가 없을 경우, 기본값으로 http://localhost:5173을 사용합니다.
     * @return UrlBasedCorsConfigurationSource 인스턴스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ALLOWED_ORIGINS 환경변수(콤마 구분) 기반 화이트리스트 CORS 설정
        String originsEnv = System.getenv("ALLOWED_ORIGINS");
        if (originsEnv == null || originsEnv.isBlank()) {
            originsEnv = "http://localhost:5173"; // 개발 환경 기본값
        }
        List<String> origins = Arrays.stream(originsEnv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info("CORS 허용 출처: {}", origins);

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        String allowedHeadersEnv = System.getenv("ALLOWED_HEADERS");
        List<String> allowedHeaders = (allowedHeadersEnv == null || allowedHeadersEnv.isBlank())
                ? DEFAULT_ALLOWED_HEADERS
                : Arrays.stream(allowedHeadersEnv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (allowedHeaders.stream().anyMatch("*"::equals)) {
            allowedHeaders = DEFAULT_ALLOWED_HEADERS;
        }

        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
