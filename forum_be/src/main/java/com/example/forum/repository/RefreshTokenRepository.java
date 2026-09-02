package com.example.forum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.forum.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(String userId);

    void deleteByUserId(String userId);

    void deleteByRefreshToken(String refreshToken);
}
