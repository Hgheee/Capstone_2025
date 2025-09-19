package com.lostfound.capstonebackend.domain.user;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.domain.user.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

/**
 * 사용자 인증 관련 REST API 컨트롤러
 * 회원가입, 로그인, 사용자 정보 관리 등의 API를 제공합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "사용자 인증 관리 API")
public class AuthController {

    private final UserService userService;
    private final com.lostfound.capstonebackend.common.util.JwtUtils jwtUtils;
    private final com.lostfound.capstonebackend.domain.auth.BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * 사용자 회원가입 API
     *
     * @param signupRequest 회원가입 요청 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다. 이메일 중복 검사와 비밀번호 암호화를 수행합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "email": "user@example.com",
                                        "name": "홍길동",
                                        "phone": "010-1234-5678",
                                        "createdAt": "2025-09-19T10:30:00",
                                        "updatedAt": "2025-09-19T10:30:00"
                                      },
                                      "error": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (입력값 검증 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "INVALID_INPUT",
                                        "message": "이메일은 필수 입력값입니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "EMAIL_ALREADY_EXISTS",
                                        "message": "이미 사용 중인 이메일입니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("POST /api/auth/signup - email: {}", signupRequest.getEmail());

        UserResponse userResponse = userService.signup(signupRequest);
        ApiResponse<UserResponse> response = ApiResponse.ok(userResponse);

        log.info("Signup successful for email: {}", signupRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자 로그인 API
     *
     * @param loginRequest 로그인 요청 정보
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "tokenType": "Bearer",
                                        "expiresIn": 86400,
                                        "issuedAt": "2025-09-19T10:30:00",
                                        "user": {
                                          "id": 1,
                                          "email": "user@example.com",
                                          "name": "홍길동",
                                          "phone": "010-1234-5678",
                                          "createdAt": "2025-09-19T10:30:00",
                                          "updatedAt": "2025-09-19T10:30:00"
                                        }
                                      },
                                      "error": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (이메일 또는 비밀번호 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "INVALID_CREDENTIALS",
                                        "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<JwtTokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("POST /api/auth/login - email: {}", loginRequest.getEmail());

        JwtTokenResponse tokenResponse = userService.login(loginRequest);
        ApiResponse<JwtTokenResponse> response = ApiResponse.ok(tokenResponse);

        log.info("Login successful for email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 이메일 중복 검사 API
     *
     * @param email 검사할 이메일
     * @return 이메일 사용 가능 여부
     */
    @GetMapping("/check-email")
    @Operation(
            summary = "이메일 중복 검사",
            description = "회원가입 시 이메일 중복 여부를 확인합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 중복 검사 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "available": true,
                                        "message": "사용 가능한 이메일입니다."
                                      },
                                      "error": null
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(@RequestParam String email) {
        log.info("GET /api/auth/check-email - email: {}", email);

        boolean available = userService.isEmailAvailable(email);
        EmailCheckResponse checkResponse = EmailCheckResponse.of(available);
        ApiResponse<EmailCheckResponse> response = ApiResponse.ok(checkResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     *
     * @return 현재 사용자 정보
     */
    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (JWT 토큰 없음 또는 무효)"
            )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String email = getAuthenticatedEmail();

        log.info("GET /api/auth/me - email: {}", email);

        UserResponse userResponse = userService.getUserByEmail(email);
        ApiResponse<UserResponse> response = ApiResponse.ok(userResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃: 현재 Access 토큰의 JTI를 블랙리스트에 등록하여 만료 전이라도 무효화합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.JWT_TOKEN_REQUIRED);
        }
        String token = authorization.substring("Bearer ".length());

        // 토큰 검증 후 JTI/만료 추출
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        String jti = jwtUtils.getJtiFromToken(token);
        java.util.Date exp = jwtUtils.getExpirationDateFromToken(token);
        if (jti == null || exp == null) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 블랙리스트 저장(중복 저장은 unique index로 방지)
        blacklistedTokenRepository.save(
                com.lostfound.capstonebackend.domain.auth.BlacklistedToken.builder()
                        .token(jti)
                        .expiresAt(java.time.LocalDateTime.ofInstant(exp.toInstant(), java.time.ZoneId.systemDefault()))
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 사용자 정보 수정 API
     *
     * @param updateRequest 수정 요청 정보
     * @return 수정된 사용자 정보
     */
    @PutMapping("/me")
    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인한 사용자의 정보를 수정합니다. JWT 토큰이 필요합니다."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        String email = getAuthenticatedEmail();

        log.info("PUT /api/auth/me - email: {}", email);

        // 현재 사용자 조회 후 업데이트
        UserResponse currentUser = userService.getUserByEmail(email);
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), updateRequest);
        ApiResponse<UserResponse> response = ApiResponse.ok(updatedUser);

        log.info("User info updated for email: {}", email);
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경 API
     *
     * @param passwordRequest 비밀번호 변경 요청 정보
     * @return 성공 메시지
     */
    @PutMapping("/change-password")
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인한 사용자의 비밀번호를 변경합니다. JWT 토큰이 필요합니다."
    )
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody PasswordChangeRequest passwordRequest) {
        String email = getAuthenticatedEmail();

        log.info("PUT /api/auth/change-password - email: {}", email);

        if (!passwordRequest.isPasswordMatched()) {
            log.warn("Password change failed - password mismatch for email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        UserResponse currentUser = userService.getUserByEmail(email);
        userService.changePassword(currentUser.getId(), passwordRequest);

        ApiResponse<String> response = ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");

        log.info("Password changed successfully for email: {}", email);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Authentication required for accessing member resource");
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authentication.getName();
    }

}
