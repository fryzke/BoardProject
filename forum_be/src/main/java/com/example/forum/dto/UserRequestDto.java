package com.example.forum.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequestDto {

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[+=%_!@#$^&*?]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자(+=%_!@#$^&*?)를 포함하여 8자 이상이어야 합니다."
    )
    private String userPassword;

    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    private String userName;
}

