package com.example.forum.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.example.forum.dto.JwtTokenDto;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private final Key key;
    private final long expiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    /**
     * 유저 ID와 Role을 기반으로 JWT 토큰을 생성
     */
    public JwtTokenDto createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date accessExpiryDate = new Date(now.getTime() + expiration);
        Date refreshExpiryDate = new Date(now.getTime() + (expiration * 168));
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(accessExpiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(refreshExpiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtTokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessExpiryDate.getTime())
                .build();
    }

    /**
     * 토큰에서 유저 ID를 추출
     */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 Role을 추출
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("auth", String.class);
    }

    /**
     * 토큰의 유효성을 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
