package com.example.forum.validator.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostTitleValidator implements ConstraintValidator<ValidPostTitle, String> {

    @Value("${post.min-title-length:2}")
    private int minLength;

    @Value("${post.max-title-length:100}")
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
                    String.format("제목은 %d자 이상 %d자 이하로 입력해주세요.", minLength, maxLength)
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}