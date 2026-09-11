package com.example.forum.dto;

import com.example.forum.validator.annotation.ValidUserName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequestDto {

    @NotBlank(message = "기존 비밀번호를 입력해주세요.")
    private String currentPassword;

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[+=%_!@#$^&*?]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자(+=%_!@#$^&*?)를 포함하여 8자 이상이어야 합니다."
    )
    private String userPassword;

    @ValidUserName
    private String userName;
}

