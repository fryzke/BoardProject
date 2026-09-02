package com.example.forum.service;

import com.example.forum.domain.Grade;
import com.example.forum.domain.RefreshToken;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.JwtTokenDto;
import com.example.forum.dto.LoginDto;
import com.example.forum.dto.SignUpDto;
import com.example.forum.repository.RefreshTokenRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.security.JwtProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[+=%_!@#$^&*?]).{8,}$";

    /**
     * 회원가입 처리
     * - userId 중복 검사
     * - 비밀번호 유효성 검사
     * - 비밀번호 BCrypt 암호화
     * - Role.USER 기본 할당
     */
    public void signUp(SignUpDto dto) {
        if (userRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        if (dto.getUserId().trim().length() < 4 || dto.getUserId().trim().length() > 16) {
            throw new IllegalArgumentException("아이디는 4자 이상 16자 이하로 입력해주세요.");
        }

        if (!dto.getUserPassword().matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자(+=%_!@#$^&*?)를 포함하여 8자 이상이어야 합니다.");
        }

        if (dto.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        User user = new User(
                dto.getUserId(),
                passwordEncoder.encode(dto.getUserPassword()),
                dto.getUserName(),
                Role.USER,
                Grade.BRONZE);

        userRepository.save(user);
    }

    /**
     * 로그인 처리
     * - 아이디로 유저 조회
     * - 비밀번호 일치 여부 확인
     * - 성공 시 JWT 토큰 발급 및 db에 리프레시 토큰 저장
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

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUserId(user.getUserId())
                .map(entity -> {
                    entity.updateRefreshToken(token.getRefreshToken());
                    return entity;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .userId(user.getUserId())
                        .refreshToken(token.getRefreshToken())
                        .build());

        refreshTokenRepository.save(refreshTokenEntity);

        return token;
    }

    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

    /*
     * 로그아웃
     * - 유효한 토큰인지 확인
     * - 리프레시 토큰 삭제
     */
    public void logOut(String refreshToken) {
        if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
            String userId = jwtProvider.getUserIdFromToken(refreshToken);
            refreshTokenRepository.findByUserId(userId)
                    .ifPresent(refreshTokenRepository::delete);
        }

    }
    /*
     * 토큰 재발급
     * - accessToken에 담긴 정보로 해당 유저 조회
     * - db에 리프레시 토큰 조회
     * - 같은 사용자인지 확인
     * - 리프레시 토큰 만료 확인
     * - 액세스 토큰 및 리프레시 토큰 재발급
     * - 새 리프레시 토큰 DB에 업데이트
     */

    public JwtTokenDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }
        // 2. 토큰에서 유저 ID 추출
        String userId = jwtProvider.getUserIdFromToken(refreshToken);

        // 3. DB에 저장된 Refresh Token 조회 및 일치 확인
        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 되었거나 존재하지 않는 토큰입니다."));
        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        // 4. 새 토큰 생성 및 DB 갱신
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        JwtTokenDto newTokenDto = jwtProvider.createToken(authentication);

        savedToken.updateRefreshToken(newTokenDto.getRefreshToken());

        return newTokenDto;
    }
}
