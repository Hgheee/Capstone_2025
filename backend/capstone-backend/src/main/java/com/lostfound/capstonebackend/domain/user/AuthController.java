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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
 * @version 1.1
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

    // ------------------------------
    // 회원가입
    // ------------------------------
    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다. 이메일 중복 검사와 비밀번호 암호화를 수행합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (입력값 검증 실패)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복"
            )
    })
    @SecurityRequirements(value = {})
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("POST /api/auth/signup - email: {}", signupRequest.getEmail());

        UserResponse userResponse = userService.signup(signupRequest);
        ApiResponse<UserResponse> response = ApiResponse.ok(userResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ------------------------------
    // 로그인
    // ------------------------------
    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 오류)")
    })
    @SecurityRequirements(value = {})
    public ResponseEntity<ApiResponse<JwtTokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtTokenResponse tokenResponse = userService.login(loginRequest);
        ApiResponse<JwtTokenResponse> response = ApiResponse.ok(tokenResponse);

        return ResponseEntity.ok(response);
    }

    // ------------------------------
    // 이메일 중복 검사
    // ------------------------------
    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 검사", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 중복 검사 완료")
    })
    @SecurityRequirements(value = {})
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        EmailCheckResponse checkResponse = EmailCheckResponse.of(available);
        return ResponseEntity.ok(ApiResponse.ok(checkResponse));
    }

    // ------------------------------
    // 내 정보 조회
    // ------------------------------
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 (JWT 토큰 없음 또는 무효)")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String email = getAuthenticatedEmail();
        UserResponse userResponse = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(userResponse));
    }

    // ------------------------------
    // 로그아웃
    // ------------------------------
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 Access 토큰을 블랙리스트에 등록하여 만료 전이라도 무효화합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT 토큰이 없거나 잘못됨")
    })
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.JWT_TOKEN_REQUIRED);
        }
        String token = authorization.substring("Bearer ".length());

        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        String jti = jwtUtils.getJtiFromToken(token);
        java.util.Date exp = jwtUtils.getExpirationDateFromToken(token);
        if (jti == null || exp == null) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        blacklistedTokenRepository.save(
                com.lostfound.capstonebackend.domain.auth.BlacklistedToken.builder()
                        .token(jti)
                        .expiresAt(java.time.LocalDateTime.ofInstant(exp.toInstant(), java.time.ZoneId.systemDefault()))
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ------------------------------
    // 내 정보 수정
    // ------------------------------
    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 정보를 수정합니다. JWT 토큰이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(@Valid @RequestBody UserUpdateRequest updateRequest) {
        String email = getAuthenticatedEmail();
        UserResponse currentUser = userService.getUserByEmail(email);
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), updateRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser));
    }

    // ------------------------------
    // 비밀번호 변경
    // ------------------------------
    @PutMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경합니다. JWT 토큰이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 불일치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody PasswordChangeRequest passwordRequest) {
        String email = getAuthenticatedEmail();

        if (!passwordRequest.isPasswordMatched()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        UserResponse currentUser = userService.getUserByEmail(email);
        userService.changePassword(currentUser.getId(), passwordRequest);

        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다."));
    }

    // ------------------------------
    // 내부 메소드: 인증된 사용자 이메일 가져오기
    // ------------------------------
    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authentication.getName();
    }

}
