package com.eduze.manage.auth.service;

import com.eduze.manage.auth.mapper.PermissionMapper;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PermissionAggregator {

    private static final String CACHE_KEY_PREFIX = "perm:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final PermissionMapper permissionMapper;
    private final StringRedisTemplate redisTemplate;

    public Set<String> aggregate(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return new LinkedHashSet<>(List.of(cached.split(",")));
        }
        List<String> codes = permissionMapper.selectPermissionCodesByUserId(userId);
        Set<String> result = new LinkedHashSet<>(codes);
        if (!result.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, String.join(",", result), CACHE_TTL);
        }
        return result;
    }

    public void evict(Long userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }
}
