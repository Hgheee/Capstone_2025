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

/**
 * 사용자 계정 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 회원가입, 로그인, 사용자 정보 조회 및 수정, 비밀번호 변경 등의 기능을 제공합니다.
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
     * 신규 사용자의 회원가입을 처리합니다.
     * 이메일 및 아이디(username) 중복 검사 후, 비밀번호를 암호화하여 사용자를 저장합니다.
     * 첫 가입자인 경우 ADMIN 권한을, 이후 가입자는 USER 권한을 부여합니다.
     *
     * @param signupRequest 회원가입에 필요한 정보(이메일, 아이디, 비밀번호, 이름, 전화번호) DTO
     * @return 생성된 사용자의 정보를 담은 DTO
     * @throws BusinessException 이메일/아이디가 이미 존재하는 경우
     */
    @Transactional
    public UserResponse signup(SignupRequest signupRequest) {
        log.info("Starting user signup process for email: {}", signupRequest.getEmail());

        // 1) 중복 검사 (이메일만)
        validateEmailNotExists(signupRequest.getEmail());
        
        // username이 제공되지 않으면 email을 사용
        String finalUsername = (signupRequest.getUsername() != null && !signupRequest.getUsername().isBlank()) 
                ? signupRequest.getUsername() 
                : signupRequest.getEmail();
        
        // username 중복 검사 (email과 다른 경우에만)
        if (!finalUsername.equals(signupRequest.getEmail())) {
            validateUsernameNotExists(finalUsername);
        }

        // 2) 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        log.debug("Password encoded for user: {}", signupRequest.getEmail());

        // 3) 역할 결정 (첫 가입자 ADMIN, 이후 USER)
        long userCount = userRepository.count();
        log.info("Current registered user count before signup: {}", userCount);
        boolean isFirstUser = userCount == 0;
        UserRole assignedRole = isFirstUser ? UserRole.ADMIN : UserRole.USER;
        if (isFirstUser) {
            log.info("First user signup detected. Assigning ADMIN role to email: {}", signupRequest.getEmail());
        } else {
            log.info("Assigning USER role to email: {}", signupRequest.getEmail());
        }

        // 4) 전화번호 정규화(하이픈 유무 모두 허용 → DB 일관 포맷으로 저장)
        String normalizedPhone = normalizePhone(signupRequest.getPhone());

        // 5) User 엔티티 구성 및 저장 (username이 없으면 email 사용)
        User user = User.builder()
                .username(finalUsername)   // ✅ email을 username으로 사용 가능
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .phone(normalizedPhone)
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User signup completed successfully for email: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        return UserResponse.from(savedUser);
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     * @param loginRequest 로그인 정보(이메일, 비밀번호) DTO
     * @return JWT 토큰 및 사용자 정보를 포함한 응답 DTO
     * @throws BusinessException 사용자 정보가 없거나 비밀번호가 일치하지 않는 경우
     */
    @Transactional
    public JwtTokenResponse login(LoginRequest loginRequest) {
        log.info("Starting user login process for email: {}", loginRequest.getEmail());

        // 1) 사용자 조회
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", loginRequest.getEmail());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", loginRequest.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3) JWT 토큰 생성 (subject = email)
        String accessToken = jwtUtils.generateToken(user.getEmail());
        log.info("Login successful for user: {}", user.getEmail());

        // 4) 응답 구성
        return JwtTokenResponse.of(
                accessToken,
                jwtUtils.getExpirationSeconds(),
                UserResponse.from(user)
        );
    }

    /**
     * 사용자 ID를 이용해 특정 사용자 정보를 조회합니다.
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
     * 사용자 이메일을 이용해 특정 사용자 정보를 조회합니다.
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
     * 특정 사용자의 정보를 수정합니다. (이름, 전화번호)
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest updateRequest) {
        log.info("Starting user update process for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update with ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        String normalizedPhone = normalizePhone(updateRequest.getPhone());
        user.updateUserInfo(updateRequest.getName(), normalizedPhone);
        User updatedUser = userRepository.save(user);

        log.info("User update completed for ID: {}", userId);
        return UserResponse.from(updatedUser);
    }

    /**
     * 특정 사용자의 비밀번호를 변경합니다.
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest passwordRequest) {
        log.info("Starting password change process for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for password change with ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(passwordRequest.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed - invalid current password for user ID: {}", userId);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String encodedNewPassword = passwordEncoder.encode(passwordRequest.getNewPassword());
        user.updatePassword(encodedNewPassword);
        userRepository.save(user);

        log.info("Password change completed for user ID: {}", userId);
    }

    /** 이메일 중복 검증 (내부용) */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Signup failed - email already exists: {}", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /** 아이디(username) 중복 검증 (내부용) */
    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("Signup failed - username already exists: {}", username);
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    /**
     * 회원가입 전, 특정 이메일이 사용 가능한지 확인합니다.
     */
    public boolean isEmailAvailable(String email) {
        boolean available = !userRepository.existsByEmail(email);
        log.debug("Email availability check for {}: {}", email, available ? "available" : "not available");
        return available;
    }

    /**
     * 아이디 찾기: 이름과 전화번호 또는 이메일로 아이디를 찾습니다.
     * @param request 아이디 찾기 요청 (이름, 전화번호 또는 이메일)
     * @return 찾은 아이디와 이메일 정보
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public FindUsernameResponse findUsername(FindUsernameRequest request) {
        log.info("Finding username for name: {}", request.getName());
        
        String phoneOrEmail = request.getPhoneOrEmail().trim();
        User user;
        
        // 이메일 형식인지 확인
        if (phoneOrEmail.contains("@")) {
            // 이메일로 검색
            String normalizedEmail = phoneOrEmail.toLowerCase();
            user = userRepository.findByNameAndEmail(request.getName(), normalizedEmail)
                    .orElseThrow(() -> {
                        log.warn("User not found - name: {}, email: {}", request.getName(), normalizedEmail);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.");
                    });
        } else {
            // 전화번호로 검색
            String normalizedPhone = normalizePhone(phoneOrEmail);
            user = userRepository.findByNameAndPhone(request.getName(), normalizedPhone)
                    .orElseThrow(() -> {
                        log.warn("User not found - name: {}, phone: {}", request.getName(), normalizedPhone);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 사용자를 찾을 수 없습니다.");
                    });
        }
        
        log.info("Username found for name: {}, username: {}", request.getName(), user.getUsername());
        return FindUsernameResponse.of(user.getUsername(), user.getEmail());
    }

    /**
     * 비밀번호 찾기: 이메일로 임시 비밀번호를 발급합니다.
     * @param request 비밀번호 찾기 요청 (이메일)
     * @return 성공 메시지
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Password reset failed - user not found: {}", request.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 이메일로 등록된 사용자를 찾을 수 없습니다.");
                });
        
        // 임시 비밀번호 생성 (8자리 랜덤)
        String tempPassword = generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        
        // 비밀번호 업데이트
        user.updatePassword(encodedPassword);
        userRepository.save(user);
        
        log.info("Temporary password generated for email: {}", request.getEmail());
        // TODO: 실제 운영 환경에서는 이메일로 임시 비밀번호를 전송해야 합니다.
        // 현재는 로그에만 출력 (보안상 실제 운영에서는 이메일 전송 필요)
        log.info("Temporary password for {}: {}", request.getEmail(), tempPassword);
        
        return "임시 비밀번호가 발급되었습니다. 이메일을 확인해주세요. (개발 환경에서는 로그를 확인하세요.)";
    }

    /**
     * 비밀번호 재설정: 이메일과 새 비밀번호로 비밀번호를 재설정합니다.
     * @param request 비밀번호 재설정 요청 (이메일, 새 비밀번호, 확인 비밀번호)
     * @return 성공 메시지
     * @throws BusinessException 사용자를 찾을 수 없거나 비밀번호가 일치하지 않는 경우
     */
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset for email: {}", request.getEmail());
        
        if (!request.isPasswordMatched()) {
            log.warn("Password reset failed - passwords do not match for email: {}", request.getEmail());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Password reset failed - user not found: {}", request.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 이메일로 등록된 사용자를 찾을 수 없습니다.");
                });
        
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedPassword);
        userRepository.save(user);
        
        log.info("Password reset successful for email: {}", request.getEmail());
        return "비밀번호가 성공적으로 재설정되었습니다.";
    }

    /**
     * 임시 비밀번호 생성 (8자리 랜덤)
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 전화번호 정규화:
     * - 입력이 null/빈문자면 그대로 반환(선택 필드 대응)
     * - 하이픈 유무 관계없이 "010-XXXX-XXXX" / "02-XXXX-XXXX" 포맷으로 변환
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;            // 선택 필드라면 null 허용
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return "";         // 빈 문자열 허용 시
        if (digits.startsWith("02")) {
            if (digits.length() <= 2) return digits;
            if (digits.length() <= 6) {
                return digits.replaceFirst("(\\d{2})(\\d{0,4})", "$1-$2");
            }
            return digits.replaceFirst("(\\d{2})(\\d{4})(\\d{0,4}).*", "$1-$2-$3");
        }
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 7) {
            return digits.replaceFirst("(\\d{3})(\\d{0,4})", "$1-$2");
        }
        return digits.replaceFirst("(\\d{3})(\\d{3,4})(\\d{0,4}).*", "$1-$2-$3");
    }
}
