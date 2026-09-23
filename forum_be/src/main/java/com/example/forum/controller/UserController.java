package com.example.forum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.forum.annotation.RateLimit;
import com.example.forum.dto.UserRequestDto;
import com.example.forum.dto.UserResponseDto;
import com.example.forum.dto.common.ApiResponse;
import com.example.forum.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /*
     * 사용자 정보 조회 API
     * GET /api/users/me
     */
    @GetMapping("/me")
    @RateLimit(capacity = 30, refillRate = 5, requested = 1)
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserInfo(@AuthenticationPrincipal String userId) {
        UserResponseDto response = userService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "회원정보가 성공적으로 조회되었습니다."));
    }

    /*
     * 사용자 정보 수정 API
     * PUT /api/users/me
     */
    @PutMapping("/me")
    @RateLimit(capacity = 5, refillRate = 1, requested = 1)
    public ResponseEntity<ApiResponse<Void>> putUserInfo(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid UserRequestDto dto) {
        userService.updateUserInfo(userId, dto);
        return ResponseEntity.ok(ApiResponse.success("회원정보가 성공적으로 수정되었습니다."));
    }

    /*
     * 회원 탈퇴 API
     * DELETE /api/users/withdraw
     */
    @DeleteMapping("/withdraw")
    @RateLimit(capacity = 3, refillRate = 1, requested = 1)
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal String userId,
            @RequestBody String userPassword) {
        userService.deleteUser(userId, userPassword);
        return ResponseEntity.ok(ApiResponse.success("성공적으로 탈퇴되었습니다."));
    }
}
