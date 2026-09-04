package com.example.forum.validator;

import org.springframework.stereotype.Component;

import com.example.forum.dto.SignUpDto;

@Component
public class AuthValidator {

    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[+=%_!@#$^&*?]).{8,}$";
    private static final int MIN_USER_ID_LENGTH = 4;
    private static final int MAX_USER_ID_LENGTH = 16;

    /**
     * 회원가입 입력값 검증
     */
    public void validateSignUp(SignUpDto dto, boolean isUserIdExists) {
        if (isUserIdExists) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        if (dto.getUserId() == null || dto.getUserId().trim().length() < MIN_USER_ID_LENGTH || dto.getUserId().trim().length() > MAX_USER_ID_LENGTH) {
            throw new IllegalArgumentException("아이디는 " + MIN_USER_ID_LENGTH + "자 이상 " + MAX_USER_ID_LENGTH + "자 이하로 입력해주세요.");
        }

        if (dto.getUserPassword() == null || !dto.getUserPassword().matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자(+=%_!@#$^&*?)를 포함하여 8자 이상이어야 합니다.");
        }

        if (dto.getUserName() == null || dto.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
    }
}
