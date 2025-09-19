package com.lostfound.capstonebackend.domain.user;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.domain.user.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 회원가입, 로그인, 사용자 정보 관리 등의 기능을 제공합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 새로운 사용자 회원가입 처리
     * 이메일 중복 검사, 비밀번호 암호화, 사용자 정보 저장을 수행합니다.
     *
     * @param signupRequest 회원가입 요청 정보
     * @return 생성된 사용자 정보 응답
     * @throws BusinessException 이메일 중복 시 EMAIL_ALREADY_EXISTS 예외
     */
    @Transactional
    public UserResponse signup(SignupRequest signupRequest) {
        log.info("Starting user signup process for email: {}", signupRequest.getEmail());

        // 1. 이메일 중복 검사
        validateEmailNotExists(signupRequest.getEmail());

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        log.debug("Password encoded for user: {}", signupRequest.getEmail());

        // 3. User 엔티티 생성 및 저장
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .phone(signupRequest.getPhone())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User signup completed successfully for email: {}", savedUser.getEmail());

        return UserResponse.from(savedUser);
    }

    /**
     * 사용자 로그인 처리
     * 이메일/비밀번호 검증 후 JWT 토큰을 발급합니다.
     *
     * @param loginRequest 로그인 요청 정보
     * @return JWT 토큰과 사용자 정보를 포함한 응답
     * @throws BusinessException 인증 실패 시 INVALID_CREDENTIALS 예외
     */
    @Transactional
    public JwtTokenResponse login(LoginRequest loginRequest) {
        log.info("Starting user login process for email: {}", loginRequest.getEmail());

        // 1. 사용자 조회
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", loginRequest.getEmail());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", loginRequest.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtUtils.generateToken(user.getEmail());
        log.info("Login successful for user: {}", user.getEmail());

        // 4. 응답 생성
        return JwtTokenResponse.of(
                accessToken,
                jwtUtils.getExpirationSeconds(),
                UserResponse.from(user)
        );
    }

    /**
     * 사용자 ID로 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보 응답
     * @throws BusinessException 사용자를 찾을 수 없는 경우 USER_NOT_FOUND 예외
     */
    public UserResponse getUserById(Long userId) {
        log.debug("Retrieving user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        return UserResponse.from(user);
    }

    /**
     * 사용자 이메일로 사용자 정보 조회
     *
     * @param email 사용자 이메일
     * @return 사용자 정보 응답
     * @throws BusinessException 사용자를 찾을 수 없는 경우 USER_NOT_FOUND 예외
     */
    public UserResponse getUserByEmail(String email) {
        log.debug("Retrieving user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        return UserResponse.from(user);
    }

    /**
     * 사용자 정보 수정
     *
     * @param userId      수정할 사용자 ID
     * @param updateRequest 수정 요청 정보
     * @return 수정된 사용자 정보 응답
     * @throws BusinessException 사용자를 찾을 수 없는 경우 USER_NOT_FOUND 예외
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest updateRequest) {
        log.info("Starting user update process for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update with ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        // 사용자 정보 업데이트
        user.updateUserInfo(updateRequest.getName(), updateRequest.getPhone());
        User updatedUser = userRepository.save(user);

        log.info("User update completed for ID: {}", userId);
        return UserResponse.from(updatedUser);
    }

    /**
     * 사용자 비밀번호 변경
     *
     * @param userId            변경할 사용자 ID
     * @param passwordRequest   비밀번호 변경 요청 정보
     * @throws BusinessException 사용자를 찾을 수 없거나 현재 비밀번호가 틀린 경우
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest passwordRequest) {
        log.info("Starting password change process for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for password change with ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(passwordRequest.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed - invalid current password for user ID: {}", userId);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 새 비밀번호 암호화 및 업데이트
        String encodedNewPassword = passwordEncoder.encode(passwordRequest.getNewPassword());
        user.updatePassword(encodedNewPassword);
        userRepository.save(user);

        log.info("Password change completed for user ID: {}", userId);
    }

    /**
     * 이메일 중복 검사
     * 회원가입 시 사용되는 내부 메소드입니다.
     *
     * @param email 검사할 이메일
     * @throws BusinessException 이메일이 이미 존재하는 경우 EMAIL_ALREADY_EXISTS 예외
     */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Signup failed - email already exists: {}", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * 이메일 중복 검사 공개 메소드
     * 프론트엔드에서 실시간 검증을 위해 사용합니다.
     *
     * @param email 검사할 이메일
     * @return true: 사용 가능한 이메일, false: 이미 사용 중인 이메일
     */
    public boolean isEmailAvailable(String email) {
        boolean available = !userRepository.existsByEmail(email);
        log.debug("Email availability check for {}: {}", email, available ? "available" : "not available");
        return available;
    }
}