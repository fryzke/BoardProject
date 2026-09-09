package com.example.forum.validator;

import org.springframework.stereotype.Component;

@Component
public class AuthValidator {

    /**
     * 회원가입 비즈니스 검증 (아이디 중복 검증)
     */
    public void validateSignUp(boolean isUserIdExists) {
        if (isUserIdExists) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
    }
}

