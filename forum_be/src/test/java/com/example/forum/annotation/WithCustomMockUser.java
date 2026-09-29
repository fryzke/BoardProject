package com.example.forum.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;

@Retention (RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUserSecurityContextFactory.class)
public @interface WithCustomMockUser {
    String userId() default "testuser1";
    String userName() default "테스트유저1";
    String password() default "Test123!";
    Role role() default Role.USER;
    Grade grade() default Grade.BRONZE;
}
