package com.example.forum.validator.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostContentValidator implements ConstraintValidator<ValidPostContent, String> {

    @Value("${post.min-content-length:1}")
    private int minLength;

    @Value("${post.max-content-length:20000}")
    private int maxLength;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.trim().length();
        if (length < minLength || length > maxLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("본문은 %d자 이상 %,d자 이하로 입력해주세요.", minLength, maxLength)
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}