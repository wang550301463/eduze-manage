package com.eduze.manage.auth.service;

import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.tenant.TenantContext;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public void checkLocked(String username) {
        if (isLocked(username)) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public boolean isLocked(String username) {
        String key = failKey(username);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        return Long.parseLong(value) >= MAX_ATTEMPTS;
    }

    public long incrementFail(String username) {
        String key = failKey(username);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, LOCK_TTL);
        }
        return count != null ? count : 0L;
    }

    public void reset(String username) {
        redisTemplate.delete(failKey(username));
    }

    private String failKey(String username) {
        return "login:fail:" + TenantContext.getTenantId() + ":" + username;
    }
}
