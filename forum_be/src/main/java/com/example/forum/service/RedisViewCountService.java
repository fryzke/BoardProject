package com.example.forum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisViewCountService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 24시간 동안 사용자/IP별 게시글 조회 이력을 체크
     * 최초 조회인 경우 true, 24시간 이내 이미 조회한 경우 false
     */
    public boolean isFirstView(Long postId, String userKey) {
        String redisKey = "view:post:" + postId + ":" + userKey;
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
        return Boolean.TRUE.equals(isFirst);
    }
}
