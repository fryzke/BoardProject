package com.example.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private boolean success;
    private String accessToken;
    private String userId;
    private String userRole;
    private String userGrade;
    private String userName;
    private String message;
}