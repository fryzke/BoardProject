package com.example.forum.controller;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.SignUpDto;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.AuthService;
import com.example.forum.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입 API
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpDto dto) {
        try {
            authService.signUp(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto) {
        try {
            JwtTokenDto token = authService.login(dto);
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                    .httpOnly(true)
                    .secure(false) // Https일시에는 true로 설정
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            User user = authService.getUser(dto.getUserId());
            userService.updateGrade(dto.getUserId());

            String userName = user != null ? user.getUserName() : dto.getUserId();
            String userRole = user != null ? user.getRole().toString() : Role.USER.toString();
            String userGrade = user != null ? user.getGrade().toString() : Grade.BRONZE.getGrade();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(Map.of(
                            "success", true,
                            "accessToken", token.getAccessToken(),
                            "userId", dto.getUserId(),
                            "userRole", userRole,
                            "userGrade", userGrade,
                            "userName", userName,
                            "message", "로그인이 완료되었습니다."
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * 로그아웃 API
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try {
            authService.logOut(refreshToken);

            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false) // HTTPS 적용 시 true
                    .path("/")
                    .maxAge(0) // 즉시 만료
                    .sameSite("Lax")
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(Map.of("success", true, "message", "로그아웃 되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /*
     * 토큰 재발급 API
     * POST /api/auth/reissue
     */

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try {
            JwtTokenDto token = authService.reissue(refreshToken);
            
            String userId = jwtProvider.getUserIdFromToken(refreshToken);
            userService.updateGrade(userId);

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                    .httpOnly(true)
                    .secure(false) // Https일시에는 true로 설정
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(Map.of("success", true, "accessToken", token.getAccessToken(), "message",
                            "토큰 재발급이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}
