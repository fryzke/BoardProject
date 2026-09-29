package com.example.forum.integration;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.repository.CommentRepository;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected FileRepository fileRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired(required = false)
    protected StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUpBase() {
        // 테스트 전 연관 데이터 정리
    }

    protected User createTestUser(String userId, String userName, String rawPassword, Role role, Grade grade) {
        return userRepository.findByUserId(userId).orElseGet(() -> {
            User user = User.builder()
                    .userId(userId)
                    .userName(userName)
                    .userPassword(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .grade(grade)
                    .build();
            return userRepository.save(user);
        });
    }

    protected Cookie createAccessTokenCookie(String userId, Role role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        JwtTokenDto tokenDto = jwtProvider.createToken(auth);
        return new Cookie("accessToken", tokenDto.getAccessToken());
    }

    protected Cookie createRefreshTokenCookie(String userId, Role role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        JwtTokenDto tokenDto = jwtProvider.createToken(auth);
        return new Cookie("refreshToken", tokenDto.getRefreshToken());
    }
}
