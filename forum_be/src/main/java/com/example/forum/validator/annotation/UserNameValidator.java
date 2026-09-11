package com.example.forum.validator.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserNameValidator implements ConstraintValidator<ValidUserName, String> {

    @Value("${auth.min-user-name-length:2}")
    private int minLength;

    @Value("${auth.max-user-name-length:20}")
    private int maxLength;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.trim().length();
        if (length == 0) {
            return true;
        }
        if (length < minLength || length > maxLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("닉네임은 %d자 이상 %d자 이하로 입력해주세요.", minLength, maxLength)
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}