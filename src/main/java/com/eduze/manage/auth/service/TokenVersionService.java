package com.eduze.manage.auth.service;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenVersionService {

    private final UserMapper userMapper;

    @Transactional
    public void bump(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        Integer current = user.getTokenVersion() == null ? 1 : user.getTokenVersion();
        user.setTokenVersion(current + 1);
        userMapper.updateById(user);
    }

    public int current(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getTokenVersion() == null) {
            return 1;
        }
        return user.getTokenVersion();
    }
}
