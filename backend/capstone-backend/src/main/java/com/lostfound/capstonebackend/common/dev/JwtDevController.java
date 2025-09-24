package com.lostfound.capstonebackend.common.dev;

import com.lostfound.capstonebackend.common.util.JwtUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/jwt")
public class JwtDevController {

    private final JwtUtils jwtUtils;

    public JwtDevController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /** 예: /api/dev/jwt/issue?email=test@ex.com */
    @GetMapping("/issue")
    public ResponseEntity<?> issue(@RequestParam String email) {
        String token = jwtUtils.generateToken(email);
        return ResponseEntity.ok(Map.of(
                "email", email,
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresInSec", jwtUtils.getExpirationSeconds()
        ));
    }

    /** Authorization: Bearer <token> 로 보내서 검증 */
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(name = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "msg", "Missing Bearer token"));
        }
        String token = auth.substring(7);
        boolean ok = jwtUtils.validateToken(token);
        return ResponseEntity.ok(Map.of(
                "ok", ok,
                "email", ok ? jwtUtils.getEmailFromToken(token) : null,
                "expired", ok ? jwtUtils.isTokenExpired(token) : true
        ));
    }
}
