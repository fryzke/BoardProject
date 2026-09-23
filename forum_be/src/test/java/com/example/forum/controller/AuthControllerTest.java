package com.example.forum.controller;

import com.example.forum.config.SecurityConfig;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.SignUpDto;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.AuthService;
import com.example.forum.service.RateLimitService;
import com.example.forum.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    /* 회원가입 API api/auth/signup */
    @DisplayName("회원가입 성공")
    @Test
    @WithMockUser
    void signUpSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        SignUpDto request = new SignUpDto();
        request.setUserId("testtestuser1");
        request.setUserName("테스트 유저");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @DisplayName("회원가입 실패:비밀번호 조건 불만족")
    @Test
    @WithMockUser
    void signUpFailPasswordInvalid() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        SignUpDto request = new SignUpDto();
        request.setUserId("testtestuser1");
        request.setUserName("테스트 유저");
        request.setUserPassword("Test1234");

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

    }

    @DisplayName("회원가입 실패:아이디 중복")
    @Test
    @WithMockUser
    void signUpFailUserIdDuplicated() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        doThrow(new IllegalArgumentException("이미 존재하는 아이디입니다."))
                .when(authService).signUp(any(SignUpDto.class));

        SignUpDto request = new SignUpDto();
        request.setUserId("testuser1");
        request.setUserName("테스트 유저");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다."));

    }

    /* 로그인 API api/auth/login */
    @DisplayName("로그인 성공: 토큰 쿠키 발급 및 유저 정보 반환")
    @Test
    @WithMockUser
    void logInSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        JwtTokenDto mockToken = JwtTokenDto.builder()
                .grantType("Bearer")
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();
        User mockUser = User.builder()
                .userId("testuser1")
                .userName("테스트유저1")
                .userPassword(passwordEncoder.encode("Test123!"))
                .role(Role.USER)
                .grade(Grade.BRONZE)
                .build();
        when(authService.login(any(LoginDto.class))).thenReturn(mockToken);
        when(authService.getUser("testuser1")).thenReturn(mockUser);

        LoginDto request = new LoginDto();
        request.setUserId("testuser1");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // 쿠키 검증
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().value("accessToken", "mock-access-token"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "mock-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                // Response Body 검증
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value("testuser1"))
                .andExpect(jsonPath("$.userName").value("테스트유저1"))
                .andExpect(jsonPath("$.userRole").value("USER"))
                .andExpect(jsonPath("$.userGrade").value("BRONZE"));

    }

    @DisplayName("로그인 실패:아이디 불일치")
    @Test
    @WithMockUser
    void logInFailWrongId() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        when(authService.login(any(LoginDto.class)))
                .thenThrow(new IllegalArgumentException("존재하지 않는 아이디입니다."));

        LoginDto request = new LoginDto();
        request.setUserId("testuser1");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 아이디입니다."));
    }

    @DisplayName("로그인 실패:비밀번호 불일치")
    @Test
    @WithMockUser
    void logInFailWrongPassword() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        when(authService.login(any(LoginDto.class)))
                .thenThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."));

        LoginDto request = new LoginDto();
        request.setUserId("testuser1");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @DisplayName("로그인 실패:탈퇴한 회원")
    @Test
    @WithMockUser
    void logInFailWithdrawUser() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        when(authService.login(any(LoginDto.class)))
                .thenThrow(new IllegalArgumentException("탈퇴한 회원입니다."));

        LoginDto request = new LoginDto();
        request.setUserId("testuser1");
        request.setUserPassword("Test123!");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("탈퇴한 회원입니다."));
    }

    /* 로그아웃 API api/auth/logout */
    @DisplayName("로그아웃 성공:쿠키 만료")
    @Test
    @WithMockUser
    void logOutSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        Cookie accessToken = new Cookie("accessToken", "mock-access-token");
        Cookie refreshToken = new Cookie("refreshToken", "mock-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessToken)
                .cookie(refreshToken))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    /* 토큰 재발급 API api/auth/reissue */
    @DisplayName("토큰 재발급 성공")
    @Test
    @WithMockUser
    void reissueSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        Cookie accessToken = new Cookie("accessToken", "mock-access-token");
        Cookie refreshToken = new Cookie("refreshToken", "mock-refresh-token");

        JwtTokenDto mockToken = JwtTokenDto.builder()
                .grantType("Bearer")
                .accessToken("mock-reissued-access-token")
                .refreshToken("mock-reissued-refresh-token")
                .build();
        when(authService.reissue(any(String.class))).thenReturn(mockToken);

        mockMvc.perform(post("/api/auth/reissue")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessToken)
                .cookie(refreshToken))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().value("accessToken", "mock-reissued-access-token"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "mock-reissued-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @DisplayName("토큰 재발급 실패")
    @Test
    @WithMockUser
    void reissueFail() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        Cookie refreshToken = new Cookie("refreshToken", "mock-invalid-refresh-token");

        when(authService.reissue(any(String.class)))
                .thenThrow(new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다."));

        mockMvc.perform(post("/api/auth/reissue")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않거나 만료된 Refresh Token입니다."));
    }
}
