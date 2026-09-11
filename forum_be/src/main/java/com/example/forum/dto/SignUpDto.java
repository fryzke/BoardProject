package com.example.forum.dto;

import com.example.forum.validator.annotation.ValidUserId;
import com.example.forum.validator.annotation.ValidUserName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    @ValidUserId
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[+=%_!@#$^&*?]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자(+=%_!@#$^&*?)를 포함하여 8자 이상이어야 합니다."
    )
    private String userPassword;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @ValidUserName
    private String userName;
}

