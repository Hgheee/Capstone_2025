package com.lostfound.capstonebackend.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.exception.GlobalExceptionHandler;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.config.JwtAuthenticationFilter;
import com.lostfound.capstonebackend.config.SecurityConfig;
import com.lostfound.capstonebackend.config.TestSecurityConfig;
import com.lostfound.capstonebackend.domain.auth.BlacklistedToken;
import com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository;
import com.lostfound.capstonebackend.domain.user.AuthController;
import com.lostfound.capstonebackend.domain.user.UserService;
import com.lostfound.capstonebackend.domain.user.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class, AuthControllerTest.MockConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(userService, jwtUtils, blacklistedTokenRepository);
    }

    @Test
    @DisplayName("로그인 성공 - JWT 토큰 반환")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("admin@example.com", "password123!");
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("admin@example.com")
                .name("관리자")
                .role(com.lostfound.capstonebackend.domain.user.UserRole.ADMIN)
                .build();
        JwtTokenResponse tokenResponse = JwtTokenResponse.of("access-token", 900L, userResponse);
        given(userService.login(any(LoginRequest.class))).willReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격증명")
    void loginFailureInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-pass");
        given(userService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    @DisplayName("회원가입 성공 - 201 반환")
    void signupSuccess() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("new@example.com");
        request.setPassword("Newpass1!");
        request.setName("새 사용자");
        request.setPhone("010-1111-2222");

        UserResponse userResponse = UserResponse.builder()
                .id(10L)
                .email("new@example.com")
                .name("새 사용자")
                .build();
        given(userService.signup(any(SignupRequest.class))).willReturn(userResponse);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("new@example.com"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signupFailureDuplicateEmail() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("DupPass1!");
        request.setName("기존 사용자");

        given(userService.signup(any(SignupRequest.class)))
                .willThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("이메일 중복 검사 - 사용 가능")
    void checkEmailAvailable() throws Exception {
        given(userService.isEmailAvailable("unique@example.com")).willReturn(true);

        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", "unique@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰 블랙리스트 저장")
    void logoutSuccess() throws Exception {
        String token = "Bearer valid-token";
        given(jwtUtils.validateToken("valid-token")).willReturn(true);
        given(jwtUtils.getJtiFromToken("valid-token")).willReturn("jti-1234");
        Date expiry = Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        given(jwtUtils.getExpirationDateFromToken("valid-token")).willReturn(expiry);

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        verify(blacklistedTokenRepository).save(any(BlacklistedToken.class));
    }

    @Test
    @DisplayName("로그아웃 실패 - Authorization 헤더 없음")
    void logoutFailsWhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.JWT_TOKEN_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 인증된 사용자")
    void getCurrentUserSuccess() throws Exception {
        String email = "user@example.com";
        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .email(email)
                .name("홍길동")
                .build();
        given(userService.getUserByEmail(email)).willReturn(userResponse);

        mockMvc.perform(get("/api/auth/me").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateCurrentUserSuccess() throws Exception {
        String email = "user@example.com";
        UserResponse currentUser = UserResponse.builder()
                .id(5L)
                .email(email)
                .name("기존")
                .build();
        UserUpdateRequest request = new UserUpdateRequest("새 이름", "010-2222-3333");
        UserResponse updated = UserResponse.builder()
                .id(5L)
                .email(email)
                .name("새 이름")
                .phone("010-2222-3333")
                .build();

        given(userService.getUserByEmail(email)).willReturn(currentUser);
        given(userService.updateUser(eq(5L), any(UserUpdateRequest.class))).willReturn(updated);

        mockMvc.perform(put("/api/auth/me")
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새 이름"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 불일치")
    void changePasswordMismatch() throws Exception {
        String email = "user@example.com";
        PasswordChangeRequest request = new PasswordChangeRequest("current!1", "newpass1!", "different!");

        mockMvc.perform(put("/api/auth/change-password")
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() throws Exception {
        String email = "user@example.com";
        PasswordChangeRequest request = new PasswordChangeRequest("current!1", "newpass1!", "newpass1!");
        UserResponse userResponse = UserResponse.builder()
                .id(7L)
                .email(email)
                .name("사용자")
                .build();

        given(userService.getUserByEmail(email)).willReturn(userResponse);

        mockMvc.perform(put("/api/auth/change-password")
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("비밀번호가 성공적으로 변경되었습니다."));

        verify(userService).changePassword(eq(7L), any(PasswordChangeRequest.class));
    }

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        @Primary
        JwtUtils jwtUtils() {
            return mock(JwtUtils.class);
        }

        @Bean
        @Primary
        BlacklistedTokenRepository blacklistedTokenRepository() {
            return mock(BlacklistedTokenRepository.class);
        }
    }
}
