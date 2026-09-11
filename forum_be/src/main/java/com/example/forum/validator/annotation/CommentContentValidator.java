package com.example.forum.validator.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommentContentValidator implements ConstraintValidator<ValidCommentContent, String> {

    @Value("${comment.max-content-length:400}")
    private int maxLength;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.trim().length();
        if (length > maxLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("댓글은 최대 %d자 이하로 입력해주세요.", maxLength)
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}