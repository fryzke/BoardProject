package com.example.forum.service;

import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.SignUpDto;
import com.example.forum.repository.UserRepository;
import com.example.forum.security.JwtProvider;
import com.example.forum.validator.AuthValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String RT_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthValidator authValidator;

    /**
     * 회원가입 처리
     * - userId 중복 검사
     * - 비밀번호 유효성 검사
     * - 비밀번호 BCrypt 암호화
     * - Role.USER 기본 할당
     */
    public void signUp(SignUpDto dto) {
        authValidator.validateSignUp(userRepository.existsByUserId(dto.getUserId()), dto.getUserId(), dto.getUserName());

        User user = User.builder()
                .userId(dto.getUserId().trim())
                .userPassword(passwordEncoder.encode(dto.getUserPassword()))
                .userName(dto.getUserName().trim())
                .role(Role.USER)
                .grade(Grade.BRONZE)
                .build();

        userRepository.save(user);
    }

    /**
     * 로그인 처리
     * - 아이디로 유저 조회
     * - 비밀번호 일치 여부 확인
     * - 성공 시 JWT 토큰 발급 및 Redis에 Refresh Token(TTL 적용) 저장
     */
    public JwtTokenDto login(LoginDto dto) {
        User user = userRepository.findByUserId(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(dto.getUserPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        JwtTokenDto token = jwtProvider.createToken(authentication);

        // Redis에 Refresh Token 저장 (TTL: 7일)
        stringRedisTemplate.opsForValue().set(
                RT_PREFIX + user.getUserId(),
                token.getRefreshToken(),
                Duration.ofMillis(jwtProvider.getRefreshExpiration())
        );

        return token;
    }

    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

    /**
     * 로그아웃
     * 1. Redis에서 Refresh Token 삭제
     * 2. Access Token을 Redis 블랙리스트에 등록하여 남은 만료시간 동안 재사용 차단
     */
    public void logOut(String accessToken, String refreshToken) {
        // 1. Refresh Token 삭제
        if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
            String userId = jwtProvider.getUserIdFromToken(refreshToken);
            stringRedisTemplate.delete(RT_PREFIX + userId);
        }

        // 2. Access Token 블랙리스트 등록
        if (accessToken != null && jwtProvider.validateToken(accessToken)) {
            long remainingTime = jwtProvider.getRemainingExpiration(accessToken);
            if (remainingTime > 0) {
                stringRedisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken,
                        "logout",
                        Duration.ofMillis(remainingTime)
                );
                log.info("AccessToken 블랙리스트 등록 완료 (남은 유효시간: {}ms)", remainingTime);
            }
        }
    }

    /**
     * 토큰 재발급
     * - Refresh Token 유효성 검증
     * - Redis에 저장된 Refresh Token과 일치 여부 확인
     * - 새 Access Token 및 Refresh Token 발급 후 Redis 갱신
     */
    public JwtTokenDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. 토큰에서 유저 ID 추출
        String userId = jwtProvider.getUserIdFromToken(refreshToken);

        // 3. Redis에 저장된 Refresh Token 조회 및 일치 확인
        String savedRefreshToken = stringRedisTemplate.opsForValue().get(RT_PREFIX + userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("로그아웃 되었거나 존재하지 않는 토큰입니다.");
        }

        // 4. 새 토큰 생성 및 Redis 갱신
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        JwtTokenDto newTokenDto = jwtProvider.createToken(authentication);

        stringRedisTemplate.opsForValue().set(
                RT_PREFIX + userId,
                newTokenDto.getRefreshToken(),
                Duration.ofMillis(jwtProvider.getRefreshExpiration())
        );

        return newTokenDto;
    }
}
