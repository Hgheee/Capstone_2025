package com.lostfound.capstonebackend.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 * 회원가입, 로그인 REST API에 대한 테스트를 수행합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserRepository userRepository;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private JwtTokenResponse jwtTokenResponse;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("test123!");
        signupRequest.setName("테스트사용자");
        signupRequest.setPhone("010-1234-5678");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("test123!");

        userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트사용자")
                .phone("010-1234-5678")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        jwtTokenResponse = JwtTokenResponse.builder()
                .accessToken("jwt_token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .issuedAt(LocalDateTime.now())
                .user(userResponse)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() throws Exception {
        // Given
        given(userService.signup(any(SignupRequest.class))).willReturn(userResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("테스트사용자"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 입력값 검증 오류")
    void signup_Fail_ValidationError() throws Exception {
        // Given
        SignupRequest invalidRequest = new SignupRequest();
        invalidRequest.setEmail("invalid-email"); // 잘못된 이메일 형식
        invalidRequest.setPassword("123"); // 너무 짧은 비밀번호
        invalidRequest.setName(""); // 빈 이름

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailAlreadyExists() throws Exception {
        // Given
        given(userService.signup(any(SignupRequest.class)))
                .willThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS));

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));

        verify(userService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // Given
        given(userService.login(any(LoginRequest.class))).willReturn(jwtTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt_token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        verify(userService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 인증 실패")
    void login_Fail_InvalidCredentials() throws Exception {
        // Given
        given(userService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        verify(userService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("이메일 중복 검사 - 사용 가능")
    void checkEmail_Available() throws Exception {
        // Given
        String email = "new@example.com";
        given(userService.isEmailAvailable(email)).willReturn(true);

        // When & Then
        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.message").value("사용 가능한 이메일입니다."));

        verify(userService).isEmailAvailable(email);
    }

    @Test
    @DisplayName("이메일 중복 검사 - 사용 불가능")
    void checkEmail_NotAvailable() throws Exception {
        // Given
        String email = "existing@example.com";
        given(userService.isEmailAvailable(email)).willReturn(false);

        // When & Then
        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("이미 사용 중인 이메일입니다."));

        verify(userService).isEmailAvailable(email);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("내 정보 조회 성공")
    void getCurrentUser_Success() throws Exception {
        // Given
        given(userService.getUserByEmail("test@example.com")).willReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("테스트사용자"));

        verify(userService).getUserByEmail("test@example.com");
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증 없음")
    void getCurrentUser_Fail_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserByEmail(anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("내 정보 수정 성공")
    void updateCurrentUser_Success() throws Exception {
        // Given
        UserUpdateRequest updateRequest = new UserUpdateRequest("수정된이름", "010-9999-8888");
        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("수정된이름")
                .phone("010-9999-8888")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userService.getUserByEmail("test@example.com")).willReturn(userResponse);
        given(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).willReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("수정된이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"));

        verify(userService).getUserByEmail("test@example.com");
        verify(userService).updateUser(eq(1L), any(UserUpdateRequest.class));
        @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(registry -> registry.anyRequest().permitAll());
            return http.build();
        }
    }

}
}