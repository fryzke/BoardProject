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

                ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token.getAccessToken())
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(30 * 60)
                                .sameSite(cookieSameSite)
                                .build();

                User user = authService.getUser(dto.getUserId());
                userService.updateGrade(dto.getUserId());

                String userName = user != null ? user.getUserName() : dto.getUserId();
                String userRole = user != null ? user.getRole().toString() : Role.USER.toString();
                String userGrade = user != null ? user.getGrade().toString() : Grade.BRONZE.getGrade();

                LoginResponseDto response = LoginResponseDto.builder()
                                .success(true)
                                .userId(dto.getUserId())
                                .userRole(userRole)
                                .userGrade(userGrade)
                                .userName(userName)
                                .message("로그인이 완료되었습니다.")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .body(response);
        }

        /**
         * 로그아웃 API
         * POST /api/auth/logout
         */
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @CookieValue(name = "accessToken", required = false) String accessToken,
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {

                authService.logOut(accessToken, refreshToken);

                ResponseCookie deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(0) // 즉시 만료
                                .sameSite(cookieSameSite)
                                .build();
                ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite(cookieSameSite)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, deleteAccessTokenCookie.toString())
                                .body(ApiResponse.success("로그아웃 되었습니다."));
        }

        /**
         * 토큰 재발급 API
         * POST /api/auth/reissue
         */
        @PostMapping("/reissue")
        public ResponseEntity<ReissueResponseDto> reissue(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {
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

                ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token.getAccessToken())
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(30 * 60)
                                .sameSite(cookieSameSite)
                                .build();

                ReissueResponseDto response = ReissueResponseDto.builder()
                                .success(true)
                                .message("토큰 재발급이 완료되었습니다.")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .body(response);
        }
}
