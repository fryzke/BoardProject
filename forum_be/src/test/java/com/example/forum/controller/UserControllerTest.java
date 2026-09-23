package com.example.forum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.UserResponseDto;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.AuthService;
import com.example.forum.service.RateLimitService;
import com.example.forum.service.UserService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthController authController;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    /* 사용자 정보 조회 API /api/users/me */
    @DisplayName("사용자 정보 조회 성공")
    @Test
    @WithMockUser
    void getUserInfoSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        UserResponseDto response = UserResponseDto.builder()
                .userId("testuser1")
                .userName("테스트유저1")
                .grade(Grade.BRONZE.toString())
                .role(Role.USER.toString())
                .createdAt(LocalDateTime.now())
                .postCount(1L)
                .commentCount(2L)
                .build();

        when(userService.getUserInfo(any(String.class))).thenReturn(response);

        /*mockMvc.perform(get("/api/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));*/
    }
    /* 사용자 정보 수정 API /api/users/me */

    /* 회원 탈퇴 API /api/users/withdraw */
}
