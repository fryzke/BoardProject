package com.example.forum.integration;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.SignUpDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @DisplayName("[통합] 회원가입 -> 로그인 -> 토큰 발급 -> 로그아웃 전체 흐름 테스트")
    @Test
    void authFullLifecycleIntegrationTest() throws Exception {
        // 1. 회원가입
        SignUpDto signUpDto = new SignUpDto();
        signUpDto.setUserId("integrationuser1");
        signUpDto.setUserName("통합테스터");
        signUpDto.setUserPassword("Password123!");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DB에 유저가 저장되었는지 확인
        User savedUser = userRepository.findByUserId("integrationuser1").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUserName()).isEqualTo("통합테스터");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getGrade()).isEqualTo(Grade.BRONZE);

        // 2. 로그인 시도
        LoginDto loginDto = new LoginDto();
        loginDto.setUserId("integrationuser1");
        loginDto.setUserPassword("Password123!");

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value("integrationuser1"))
                .andExpect(jsonPath("$.userName").value("통합테스터"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        Cookie accessTokenCookie = loginResult.getResponse().getCookie("accessToken");
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(refreshTokenCookie).isNotNull();

        // 3. 토큰 재발급
        var reissueResult = mockMvc.perform(post("/api/auth/reissue")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        Cookie newAccessToken = reissueResult.getResponse().getCookie("accessToken");
        assertThat(newAccessToken).isNotNull();

        // 4. 로그아웃
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessTokenCookie)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @DisplayName("[통합] 중복 아이디 회원가입 시 실패 (400 Bad Request)")
    @Test
    void signUpFailDuplicatedUserId() throws Exception {
        createTestUser("dupuser1", "기존유저", "Password123!", Role.USER, Grade.BRONZE);

        SignUpDto signUpDto = new SignUpDto();
        signUpDto.setUserId("dupuser1");
        signUpDto.setUserName("신규유저");
        signUpDto.setUserPassword("Password123!");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @DisplayName("[통합] 잘못된 비밀번호로 로그인 시 실패 (400 Bad Request)")
    @Test
    void loginFailWrongPassword() throws Exception {
        createTestUser("loginuser1", "로그인유저", "Password123!", Role.USER, Grade.BRONZE);

        LoginDto loginDto = new LoginDto();
        loginDto.setUserId("loginuser1");
        loginDto.setUserPassword("WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
