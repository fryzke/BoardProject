package com.example.forum.controller;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.LoginResponseDto;
import com.example.forum.dto.ReissueResponseDto;
import com.example.forum.dto.SignUpDto;
import com.example.forum.dto.common.ApiResponse;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.AuthService;
import com.example.forum.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @org.springframework.beans.factory.annotation.Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @org.springframework.beans.factory.annotation.Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * 회원가입 API
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody @Valid SignUpDto dto) {
        authService.signUp(dto);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginDto dto) {
        JwtTokenDto token = authService.login(dto);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(cookieSameSite)
                .build();

        User user = authService.getUser(dto.getUserId());
        userService.updateGrade(dto.getUserId());

        String userName = user != null ? user.getUserName() : dto.getUserId();
        String userRole = user != null ? user.getRole().toString() : Role.USER.toString();
        String userGrade = user != null ? user.getGrade().toString() : Grade.BRONZE.getGrade();

        LoginResponseDto response = LoginResponseDto.builder()
                .success(true)
                .accessToken(token.getAccessToken())
                .userId(dto.getUserId())
                .userRole(userRole)
                .userGrade(userGrade)
                .userName(userName)
                .message("로그인이 완료되었습니다.")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    /**
     * 로그아웃 API
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearerToken,
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        String accessToken = null;
        if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }

        authService.logOut(accessToken, refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0) // 즉시 만료
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success("로그아웃 되었습니다."));
    }

    /**
     * 토큰 재발급 API
     * POST /api/auth/reissue
     */
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponseDto> reissue(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        JwtTokenDto token = authService.reissue(refreshToken);

        String userId = jwtProvider.getUserIdFromToken(refreshToken);
        userService.updateGrade(userId);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(cookieSameSite)
                .build();

        ReissueResponseDto response = ReissueResponseDto.builder()
                .success(true)
                .accessToken(token.getAccessToken())
                .message("토큰 재발급이 완료되었습니다.")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }
}
