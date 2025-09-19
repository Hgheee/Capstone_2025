package com.lostfound.capstonebackend.domain.user;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 * 회원가입, 로그인, 사용자 관리 기능에 대한 테스트를 수행합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User testUser;

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

        testUser = User.builder()
                .email("test@example.com")
                .password("encoded_password")
                .name("테스트사용자")
                .phone("010-1234-5678")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // Given
        given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encoded_password");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        UserResponse result = userService.signup(signupRequest);

        // Then
        assertThat(result.getEmail()).isEqualTo(signupRequest.getEmail());
        assertThat(result.getName()).isEqualTo(signupRequest.getName());
        assertThat(result.getPhone()).isEqualTo(signupRequest.getPhone());

        verify(userRepository).existsByEmail(signupRequest.getEmail());
        verify(passwordEncoder).encode(signupRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailAlreadyExists() {
        // Given
        given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.signup(signupRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository).existsByEmail(signupRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // Given
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).willReturn(true);
        given(jwtUtils.generateToken(testUser.getEmail())).willReturn("jwt_token");
        given(jwtUtils.getExpirationSeconds()).willReturn(86400L);

        // When
        JwtTokenResponse result = userService.login(loginRequest);

        // Then
        assertThat(result.getAccessToken()).isEqualTo("jwt_token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(86400L);
        assertThat(result.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtils).generateToken(testUser.getEmail());
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Fail_UserNotFound() {
        // Given
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_InvalidPassword() {
        // Given
        given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void getUserById_Success() {
        // Given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getName()).isEqualTo(testUser.getName());

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 - 사용자 없음")
    void getUserById_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void getUserByEmail_Success() {
        // Given
        String email = "test@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));

        // When
        UserResponse result = userService.getUserByEmail(email);

        // Then
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getName()).isEqualTo(testUser.getName());

        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("이메일 중복 검사 - 사용 가능")
    void isEmailAvailable_Available() {
        // Given
        String email = "new@example.com";
        given(userRepository.existsByEmail(email)).willReturn(false);

        // When
        boolean result = userService.isEmailAvailable(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("이메일 중복 검사 - 사용 불가능")
    void isEmailAvailable_NotAvailable() {
        // Given
        String email = "existing@example.com";
        given(userRepository.existsByEmail(email)).willReturn(true);

        // When
        boolean result = userService.isEmailAvailable(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공")
    void updateUser_Success() {
        // Given
        Long userId = 1L;
        UserUpdateRequest updateRequest = new UserUpdateRequest("수정된이름", "010-9999-8888");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        UserResponse result = userService.updateUser(userId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // Given
        Long userId = 1L;
        PasswordChangeRequest passwordRequest = new PasswordChangeRequest(
                "currentPassword", "newPassword123!", "newPassword123!");

        String currentEncodedPassword = testUser.getPassword();
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("currentPassword", currentEncodedPassword)).willReturn(true);
        given(passwordEncoder.encode("newPassword123!")).willReturn("encoded_new_password");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        userService.changePassword(userId, passwordRequest);

        // Then
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches("currentPassword", currentEncodedPassword);
        verify(passwordEncoder).encode("newPassword123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_Fail_InvalidCurrentPassword() {
        // Given
        Long userId = 1L;
        PasswordChangeRequest passwordRequest = new PasswordChangeRequest(
                "wrongPassword", "newPassword123!", "newPassword123!");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrongPassword", testUser.getPassword())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, passwordRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches("wrongPassword", testUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}