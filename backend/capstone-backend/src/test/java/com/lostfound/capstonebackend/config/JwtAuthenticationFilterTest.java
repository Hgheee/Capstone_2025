package com.lostfound.capstonebackend.config;

import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import com.lostfound.capstonebackend.domain.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
            jwtUtils,
            userRepository,
            blacklistedTokenRepository
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 성공")
    void authenticateWithValidToken() throws Exception {
        // Given
        String token = "valid-jwt-token";
        String email = "test@test.com";
        User user = User.builder()
            .email(email)
            .password("password")
            .name("테스트")
            .username("testuser")
            .role(UserRole.USER)
            .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/lost-items");
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getJtiFromToken(token)).thenReturn(null);
        when(jwtUtils.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(blacklistedTokenRepository.existsByToken(anyString())).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(email);
        assertThat(authentication.getAuthorities()).hasSize(1);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 인증 실패")
    void authenticateWithExpiredToken() throws Exception {
        // Given
        String token = "expired-jwt-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/lost-items");
        when(jwtUtils.validateToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰으로 인증 실패")
    void authenticateWithInvalidTokenFormat() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");
        when(request.getRequestURI()).thenReturn("/api/lost-items");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtUtils, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("JWT 토큰 없이 보호된 API 접근 실패")
    void accessProtectedEndpointWithoutToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/lost-items");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtUtils, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("블랙리스트에 등록된 토큰으로 인증 실패")
    void authenticateWithBlacklistedToken() throws Exception {
        // Given
        String token = "blacklisted-token";
        String jti = "token-jti";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/lost-items");
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getJtiFromToken(token)).thenReturn(jti);
        when(blacklistedTokenRepository.existsByToken(jti)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("유효한 토큰이지만 사용자를 찾을 수 없는 경우")
    void authenticateWithValidTokenButUserNotFound() throws Exception {
        // Given
        String token = "valid-jwt-token";
        String email = "nonexistent@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/lost-items");
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getJtiFromToken(token)).thenReturn(null);
        when(jwtUtils.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(blacklistedTokenRepository.existsByToken(anyString())).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("공개 엔드포인트는 필터 적용 안 함 - 로그인")
    void shouldNotFilterLoginEndpoint() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // When
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("공개 엔드포인트는 필터 적용 안 함 - 회원가입")
    void shouldNotFilterSignupEndpoint() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/signup");

        // When
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("공개 엔드포인트는 필터 적용 안 함 - Health Check")
    void shouldNotFilterHealthCheckEndpoint() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/health");

        // When
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("보호된 엔드포인트는 필터 적용")
    void shouldFilterProtectedEndpoint() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/lost-items");

        // When
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isFalse();
    }

    @Test
    @DisplayName("ADMIN 권한 사용자 인증 성공")
    void authenticateAdminUser() throws Exception {
        // Given
        String token = "admin-jwt-token";
        String email = "admin@test.com";
        User admin = User.builder()
            .email(email)
            .password("password")
            .name("관리자")
            .username("admin")
            .role(UserRole.ADMIN)
            .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/admin");
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getJtiFromToken(token)).thenReturn(null);
        when(jwtUtils.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(admin));
        when(blacklistedTokenRepository.existsByToken(anyString())).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(email);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority())
            .isEqualTo("ROLE_ADMIN");

        verify(filterChain).doFilter(request, response);
    }
}
