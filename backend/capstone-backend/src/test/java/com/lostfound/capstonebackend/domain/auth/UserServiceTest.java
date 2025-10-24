package com.lostfound.capstonebackend.domain.auth;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import com.lostfound.capstonebackend.domain.user.UserRole;
import com.lostfound.capstonebackend.domain.user.UserService;
import com.lostfound.capstonebackend.domain.user.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 첫 사용자 ADMIN 권한 부여")
    void signupFirstUserBecomesAdmin() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setEmail("first@example.com");
        request.setPassword("Password1!");
        request.setName("첫 사용자");
        request.setPhone("010-1111-2222");

        given(userRepository.existsByEmail("first@example.com")).willReturn(false);
        given(passwordEncoder.encode("Password1!")).willReturn("encoded");
        given(userRepository.count()).willReturn(0L);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
            ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now());
            return user;
        });

        // When
        UserResponse response = userService.signup(request);

        // Then
        assertThat(response.getEmail()).isEqualTo("first@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        verify(passwordEncoder).encode("Password1!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signupFailsWhenEmailExists() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setEmail("dup@example.com");
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Nested
    @DisplayName("로그인")
    class Login {
        @Test
        @DisplayName("로그인 성공 - 토큰 발급")
        void loginSuccess() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "Password1!");
            User user = buildUser(2L, "user@example.com", "encoded", UserRole.USER);
            given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", "encoded")).willReturn(true);
            given(jwtUtils.generateToken("user@example.com")).willReturn("jwt-token");
            given(jwtUtils.getExpirationSeconds()).willReturn(900L);

            // When
            JwtTokenResponse response = userService.login(request);

            // Then
            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getExpiresIn()).isEqualTo(900L);
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("로그인 실패 - 사용자 없음")
        void loginFailsWhenUserMissing() {
            // Given
            LoginRequest request = new LoginRequest("missing@example.com", "Password1!");
            given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void loginFailsWhenPasswordMismatch() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "wrong");
            User user = buildUser(3L, "user@example.com", "encoded", UserRole.USER);
            given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 성공")
    void getUserByIdSuccess() {
        // Given
        User user = buildUser(4L, "user@example.com", "encoded", UserRole.USER);
        given(userRepository.findById(4L)).willReturn(Optional.of(user));

        // When
        UserResponse response = userService.getUserById(4L);

        // Then
        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 실패")
    void getUserByIdNotFound() {
        // Given
        given(userRepository.findById(9L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(9L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 - 성공")
    void getUserByEmailSuccess() {
        // Given
        User user = buildUser(6L, "lookup@example.com", "encoded", UserRole.USER);
        given(userRepository.findByEmail("lookup@example.com")).willReturn(Optional.of(user));

        // When
        UserResponse response = userService.getUserByEmail("lookup@example.com");

        // Then
        assertThat(response.getId()).isEqualTo(6L);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 - 실패")
    void getUserByEmailNotFound() {
        // Given
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail("missing@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 정보 수정 - 성공")
    void updateUserSuccess() {
        // Given
        User user = buildUser(7L, "user@example.com", "encoded", UserRole.USER);
        user.updateUserInfo("기존", "010-0000-0000");
        UserUpdateRequest request = new UserUpdateRequest("수정", "010-1234-5678");
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);

        // When
        UserResponse response = userService.updateUser(7L, request);

        // Then
        assertThat(response.getName()).isEqualTo("수정");
        assertThat(response.getPhone()).isEqualTo("010-1234-5678");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("사용자 정보 수정 - 대상 없음")
    void updateUserNotFound() {
        // Given
        UserUpdateRequest request = new UserUpdateRequest("수정", "010-1234-5678");
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() {
        // Given
        User user = buildUser(8L, "user@example.com", "encoded", UserRole.USER);
        PasswordChangeRequest request = new PasswordChangeRequest("old", "newPass1!", "newPass1!");
        given(userRepository.findById(8L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("old", "encoded")).willReturn(true);
        given(passwordEncoder.encode("newPass1!")).willReturn("encoded-new");
        given(userRepository.save(user)).willReturn(user);

        // When
        userService.changePassword(8L, request);

        // Then
        verify(userRepository).save(user);
        verify(passwordEncoder).encode("newPass1!");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePasswordFailsWhenCurrentMismatch() {
        // Given
        User user = buildUser(8L, "user@example.com", "encoded", UserRole.USER);
        PasswordChangeRequest request = new PasswordChangeRequest("wrong", "newPass1!", "newPass1!");
        given(userRepository.findById(8L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(8L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("이메일 사용 가능 여부 확인")
    void isEmailAvailable() {
        // Given
        given(userRepository.existsByEmail("free@example.com")).willReturn(false);

        // When & Then
        assertThat(userService.isEmailAvailable("free@example.com")).isTrue();
    }

    private User buildUser(Long id, String email, String password, UserRole role) {
        User user = User.builder()
                .email(email)
                .password(password)
                .name("사용자")
                .phone("010-1234-5678")
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now());
        return user;
    }
}
