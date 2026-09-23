package com.example.forum.service;

import java.time.Instant;
import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public boolean isAllowed(String keySuffix, long capacity, long refillRate, long requested) {
        String key = "ratelimit:bucket:" + keySuffix;
        long currentTimeStamp = Instant.now().getEpochSecond();

        Long result = redisTemplate.execute(
            tokenBucketScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(currentTimeStamp),
            String.valueOf(requested)
        );

        return result != null && result == 1L;
    }
}
