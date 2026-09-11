package com.example.forum.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator {

    @Value("${auth.min-user-id-length:4}")
    private int minUserIdLength;

    @Value("${auth.max-user-id-length:16}")
    private int maxUserIdLength;

    @Value("${auth.min-user-name-length:2}")
    private int minUserNameLength;

    @Value("${auth.max-user-name-length:20}")
    private int maxUserNameLength;

    /**
     * 회원가입 비즈니스 검증 (아이디 중복 및 길이 검증)
     */
    public void validateSignUp(boolean isUserIdExists, String userId, String userName) {
        if (isUserIdExists) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        validateUserId(userId);
        validateUserName(userName);
    }

    public void validateSignUp(boolean isUserIdExists) {
        validateSignUp(isUserIdExists, null, null);
    }

    public void validateUserId(String userId) {
        if (userId != null) {
            int len = userId.trim().length();
            if (len < minUserIdLength || len > maxUserIdLength) {
                throw new IllegalArgumentException(String.format("아이디는 %d자 이상 %d자 이하로 입력해주세요.", minUserIdLength, maxUserIdLength));
            }
        }
    }

    public void validateUserName(String userName) {
        if (userName != null && !userName.isBlank()) {
            int len = userName.trim().length();
            if (len < minUserNameLength || len > maxUserNameLength) {
                throw new IllegalArgumentException(String.format("닉네임은 %d자 이상 %d자 이하로 입력해주세요.", minUserNameLength, maxUserNameLength));
            }
        }
    }
}

