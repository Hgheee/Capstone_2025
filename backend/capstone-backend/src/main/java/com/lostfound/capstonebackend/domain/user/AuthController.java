package com.lostfound.capstonebackend.domain.user;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.domain.auth.BlacklistedToken;
import com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository;
import com.lostfound.capstonebackend.domain.user.dto.*;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 사용자 인증(회원가입, 로그인, 로그아웃) 및 사용자 정보 관리 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "사용자 인증 및 프로필 관리 API")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * 신규 사용자를 시스템에 등록합니다.
     * @param signupRequest 회원가입에 필요한 사용자 정보 DTO
     * @return 생성된 사용자 정보와 함께 HTTP 201 Created 응답을 반환합니다.
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 이메일 중복 검사와 비밀번호 암호화를 수행합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("POST /api/auth/signup - email: {}", signupRequest.getEmail());
        UserResponse userResponse = userService.signup(signupRequest);
        ApiResponse<UserResponse> response = ApiResponse.ok(userResponse);
        log.info("Signup successful for email: {}", signupRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자를 로그인 처리하고 JWT 토큰을 발급합니다.
     * @param loginRequest 로그인 이메일 및 비밀번호 정보 DTO
     * @return JWT 토큰 정보와 사용자 정보가 포함된 ApiResponse
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 오류)")
    })
    public ResponseEntity<ApiResponse<JwtTokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("POST /api/auth/login - email: {}", loginRequest.getEmail());
        JwtTokenResponse tokenResponse = userService.login(loginRequest);
        ApiResponse<JwtTokenResponse> response = ApiResponse.ok(tokenResponse);
        log.info("Login successful for email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 회원가입 시 이메일이 사용 가능한지 중복 여부를 확인합니다.
     * @param email 검사할 이메일 주소
     * @return 이메일 사용 가능 여부(available)가 포함된 ApiResponse
     */
    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 검사", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 중복 검사 완료", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(@RequestParam String email) {
        log.info("GET /api/auth/check-email - email: {}", email);
        boolean available = userService.isEmailAvailable(email);
        EmailCheckResponse checkResponse = EmailCheckResponse.of(available);
        ApiResponse<EmailCheckResponse> response = ApiResponse.ok(checkResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 인증된 사용자의 상세 정보를 조회합니다.
     * @return 현재 사용자의 정보가 포함된 ApiResponse
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 (JWT 토큰 없음 또는 무효)")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String email = getAuthenticatedEmail();
        log.info("GET /api/auth/me - email: {}", email);
        UserResponse userResponse = userService.getUserByEmail(email);
        ApiResponse<UserResponse> response = ApiResponse.ok(userResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자를 로그아웃 처리합니다.
     * 요청 헤더의 JWT를 받아, 해당 토큰의 JTI를 블랙리스트에 추가하여 토큰을 무효화합니다.
     * @param authorization 'Bearer {token}' 형식의 Authorization 헤더 값
     * @return 성공 시 데이터가 없는 200 OK 응답을 반환합니다.
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 토큰을 무효화(블랙리스트에 추가)하여 로그아웃 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.JWT_TOKEN_REQUIRED);
        }
        String token = authorization.substring("Bearer ".length());

        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        String jti = jwtUtils.getJtiFromToken(token);
        Date exp = jwtUtils.getExpirationDateFromToken(token);
        if (jti == null || exp == null) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN, "토큰에 JTI 또는 만료 시간이 없습니다.");
        }

        blacklistedTokenRepository.save(
                BlacklistedToken.builder()
                        .token(jti)
                        .expiresAt(LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault()))
                        .build()
        );
        log.info("User logged out, token JTI blacklisted: {}", jti);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 현재 인증된 사용자의 정보를 수정합니다.
     * @param updateRequest 수정할 사용자 정보(이름, 전화번호) DTO
     * @return 수정된 사용자 정보가 포함된 ApiResponse
     */
    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 정보를 수정합니다. JWT 토큰이 필요합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(@Valid @RequestBody UserUpdateRequest updateRequest) {
        String email = getAuthenticatedEmail();
        log.info("PUT /api/auth/me - email: {}", email);
        UserResponse currentUser = userService.getUserByEmail(email);
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), updateRequest);
        ApiResponse<UserResponse> response = ApiResponse.ok(updatedUser);
        log.info("User info updated for email: {}", email);
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 인증된 사용자의 비밀번호를 변경합니다.
     * @param passwordRequest 현재 비밀번호와 새 비밀번호를 담은 DTO
     * @return 성공 메시지가 포함된 ApiResponse
     */
    @PutMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경합니다. JWT 토큰이 필요합니다.")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody PasswordChangeRequest passwordRequest) {
        String email = getAuthenticatedEmail();
        log.info("PUT /api/auth/change-password - email: {}", email);

        if (!passwordRequest.isPasswordMatched()) {
            log.warn("Password change failed - new password and confirmation do not match for email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        UserResponse currentUser = userService.getUserByEmail(email);
        userService.changePassword(currentUser.getId(), passwordRequest);

        ApiResponse<String> response = ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");
        log.info("Password changed successfully for email: {}", email);
        return ResponseEntity.ok(response);
    }

    /**
     * 아이디 찾기: 이름과 전화번호 또는 이메일로 아이디를 찾습니다.
     * @param request 아이디 찾기 요청 (이름, 전화번호 또는 이메일)
     * @return 찾은 아이디 정보가 포함된 ApiResponse
     */
    @PostMapping("/find-username")
    @Operation(summary = "아이디 찾기", description = "이름과 전화번호 또는 이메일로 아이디를 찾습니다.")
    public ResponseEntity<ApiResponse<FindUsernameResponse>> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        log.info("POST /api/auth/find-username - name: {}", request.getName());
        FindUsernameResponse response = userService.findUsername(request);
        log.info("Username found for name: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 비밀번호 찾기: 이메일로 임시 비밀번호를 발급합니다.
     * @param request 비밀번호 찾기 요청 (이메일)
     * @return 성공 메시지가 포함된 ApiResponse
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "비밀번호 찾기", description = "이메일로 임시 비밀번호를 발급합니다.")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - email: {}", request.getEmail());
        String message = userService.forgotPassword(request);
        log.info("Temporary password generated for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    /**
     * 비밀번호 재설정: 이메일과 새 비밀번호로 비밀번호를 재설정합니다.
     * @param request 비밀번호 재설정 요청 (이메일, 새 비밀번호, 확인 비밀번호)
     * @return 성공 메시지가 포함된 ApiResponse
     */
    @PostMapping("/reset-password")
    @Operation(summary = "비밀번호 재설정", description = "이메일과 새 비밀번호로 비밀번호를 재설정합니다.")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password - email: {}", request.getEmail());
        String message = userService.resetPassword(request);
        log.info("Password reset successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    /**
     * SecurityContextHolder에서 현재 인증된 사용자의 이메일(principal의 name)을 가져옵니다.
     * @return 인증된 사용자의 이메일 문자열
     * @throws BusinessException 사용자가 인증되지 않았거나 익명 사용자인 경우
     */
    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Authentication required for accessing member resource");
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authentication.getName();
    }
}
