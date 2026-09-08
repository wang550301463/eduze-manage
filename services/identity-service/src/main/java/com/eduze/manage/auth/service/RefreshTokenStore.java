package com.eduze.manage.auth.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "identity:jwt:refresh:";

    private final StringRedisTemplate redisTemplate;

    public void store(String jti, Long userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), ttl);
    }

    public boolean isActive(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }

    public void revoke(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX + jti);
    }
}
