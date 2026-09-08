package com.eduze.manage.auth.service;

import com.eduze.manage.auth.mapper.PermissionMapper;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionAggregator {

    private static final String CACHE_KEY_PREFIX = "identity:perm:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final PermissionMapper permissionMapper;
    private final StringRedisTemplate redisTemplate;

    public Set<String> aggregate(Long userId) {
        // Authorization revocations must be visible immediately, not after a cache TTL.
        List<String> codes = permissionMapper.selectPermissionCodesByUserId(userId);
        Set<String> result = new LinkedHashSet<>(codes);
        return result;
    }

    public void evict(Long userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }
}
