package com.example.forum.integration;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.UserRequestDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class UserIntegrationTest extends AbstractIntegrationTest {

    @DisplayName("[통합] 내 정보 조회 및 기존 비밀번호 확인을 통한 회원 정보 수정")
    @Test
    void getUserInfoAndUpdateTest() throws Exception {
        String userId = "testuser_update";
        String rawPassword = "OldPassword123!";
        createTestUser(userId, "원래이름", rawPassword, Role.USER, Grade.BRONZE);

        Cookie authCookie = createAccessTokenCookie(userId, Role.USER);

        // 1. 내 정보 조회
        mockMvc.perform(get("/api/users/me")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.userName").value("원래이름"));

        // 2. 잘못된 기존 비밀번호로 수정 시도 -> 실패 (400)
        UserRequestDto wrongPwDto = new UserRequestDto();
        wrongPwDto.setUserName("바꿀이름");
        wrongPwDto.setCurrentPassword("WrongPassword123!");
        wrongPwDto.setUserPassword("NewPassword123!");

        mockMvc.perform(put("/api/users/me")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPwDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 3. 올바른 기존 비밀번호로 닉네임 및 비밀번호 수정 -> 성공
        UserRequestDto correctDto = new UserRequestDto();
        correctDto.setUserName("수정된이름");
        correctDto.setCurrentPassword(rawPassword);
        correctDto.setUserPassword("NewPassword123!");

        mockMvc.perform(put("/api/users/me")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DB 반영 확인
        User updatedUser = userRepository.findByUserId(userId).orElseThrow();
        assertThat(updatedUser.getUserName()).isEqualTo("수정된이름");
        assertThat(passwordEncoder.matches("NewPassword123!", updatedUser.getUserPassword())).isTrue();
    }

    @DisplayName("[통합] 회원 탈퇴 시 기존 비밀번호 검증 및 논리 삭제 처리")
    @Test
    void withdrawUserTest() throws Exception {
        String userId = "withdraw_user";
        String rawPassword = "Password123!";
        createTestUser(userId, "탈퇴예정자", rawPassword, Role.USER, Grade.BRONZE);

        Cookie authCookie = createAccessTokenCookie(userId, Role.USER);

        // 1. 비밀번호 불일치로 탈퇴 시도 -> 실패 (400)
        mockMvc.perform(delete("/api/users/withdraw")
                        .cookie(authCookie)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("WrongPassword!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 2. 올바른 비밀번호로 탈퇴 시도 -> 성공
        mockMvc.perform(delete("/api/users/withdraw")
                        .cookie(authCookie)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(rawPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DB에서 soft delete 되었는지 확인
        User deletedUser = userRepository.findByUserId(userId).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
    }
}
