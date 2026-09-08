package com.eduze.manage.auth.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserBranchMapper;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.mapper.UserRoleMapper;
import com.eduze.manage.auth.service.PermissionAggregator;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.tenant.TenantContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserBranchMapper userBranchMapper;
    private final PermissionAggregator permissionAggregator;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =
                userMapper.selectOne(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getTenantId, TenantContext.getTenantId())
                                .eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return buildDetails(user);
    }

    public CustomUserDetails loadByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return buildDetails(user);
    }

    private CustomUserDetails buildDetails(User user) {
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(user.getId());
        List<Long> branchIds = userBranchMapper.selectBranchIdsByUserId(user.getId());
        Set<String> permissions = permissionAggregator.aggregate(user.getId());
        return new CustomUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getName(),
                user.getPasswordHash(),
                branchIds,
                new HashSet<>(roleCodes),
                permissions,
                user.getTokenVersion() == null ? 1 : user.getTokenVersion(),
                user.getStatus() != null && user.getStatus() == 1);
    }
}
