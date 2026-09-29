package com.example.forum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.forum.annotation.WithCustomMockUser;
import com.example.forum.config.SecurityConfig;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.dto.UserRequestDto;
import com.example.forum.dto.UserResponseDto;
import com.example.forum.security.JwtAuthenticationFilter;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.RateLimitService;
import com.example.forum.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithCustomMockUser
@Import(SecurityConfig.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    /* 사용자 정보 조회 API /api/users/me */
    @DisplayName("사용자 정보 조회 성공")
    @Test
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

        when(userService.getUserInfo(any())).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("testuser1"))
                .andExpect(jsonPath("$.data.userName").value("테스트유저1"));

    }

    /* 사용자 정보 수정 API /api/users/me */
    @DisplayName("사용자 정보 수정 성공: 사용자이름 변경")
    @Test
    void editUserNameSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        UserRequestDto request = new UserRequestDto();
        request.setCurrentPassword("Test123!");
        request.setUserName("editedUser1");
        request.setUserPassword(null);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원정보가 성공적으로 수정되었습니다."));
    }

    @DisplayName("사용자 정보 수정 성공: 비밀번호 변경")
    @Test
    void editUserPasswordSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        UserRequestDto request = new UserRequestDto();
        request.setCurrentPassword("Test123!");
        request.setUserName(null);
        request.setUserPassword("Newtest123!");

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원정보가 성공적으로 수정되었습니다."));
    }

    @DisplayName("사용자 정보 수정 실패: 기존 비밀번호 불일치")
    @Test
    void editUserInfoFail() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        doThrow(new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다."))
                .when(userService).updateUserInfo(any(String.class), any(UserRequestDto.class));

        UserRequestDto request = new UserRequestDto();
        request.setCurrentPassword("Test1234");
        request.setUserName(null);
        request.setUserPassword("Newtest123!");

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("기존 비밀번호가 일치하지 않습니다."));
    }
    /* 회원 탈퇴 API /api/users/withdraw */

    @DisplayName("회원탈퇴 성공")
    @Test
    void withdrawUserSuccess() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        String userPassword = "Test123!";

        mockMvc.perform(delete("/api/users/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("성공적으로 탈퇴되었습니다."));
    }

    @DisplayName("회원탈퇴 실패 : 비밀번호 불일치")
    @Test
    void withdrawUserFail() throws Exception {
        when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

        doThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."))
                .when(userService).deleteUser(any(String.class), any(String.class));

        String userPassword = "Test1234";

        mockMvc.perform(delete("/api/users/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }
}
