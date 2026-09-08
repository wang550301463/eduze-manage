package com.eduze.manage.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.domain.UserBranch;
import com.eduze.manage.auth.domain.UserRole;
import com.eduze.manage.auth.dto.AssignBranchesRequest;
import com.eduze.manage.auth.dto.AssignRolesRequest;
import com.eduze.manage.auth.dto.UserCreateRequest;
import com.eduze.manage.auth.dto.UserResponse;
import com.eduze.manage.auth.dto.UserUpdateRequest;
import com.eduze.manage.auth.mapper.UserBranchMapper;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.mapper.UserRoleMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.common.web.PageResult;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserBranchMapper userBranchMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionAggregator permissionAggregator;
    private final TokenVersionService tokenVersionService;

    public PageResult<UserResponse> page(int page, int size, String keyword) {
        Page<User> result =
                userMapper.selectPage(
                        new Page<>(page, size),
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getTenantId, TenantContext.getTenantId())
                                .and(
                                        StringUtils.hasText(keyword),
                                        w ->
                                                w.like(User::getUsername, keyword)
                                                        .or()
                                                        .like(User::getName, keyword))
                                .orderByDesc(User::getCreatedAt));
        List<UserResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        User user = new User();
        user.setTenantId(TenantContext.getTenantId());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setTokenVersion(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "用户名已存在", ex);
        }
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = requireUser(id);
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);
        if (request.getStatus() != null) {
            tokenVersionService.bump(user.getId());
        }
        return toResponse(userMapper.selectById(user.getId()));
    }

    @Transactional
    public void delete(Long id) {
        User user = requireUser(id);
        userMapper.deleteById(user.getId());
        permissionAggregator.evict(id);
        tokenVersionService.bump(id);
    }

    @Transactional
    public void assignRoles(Long userId, AssignRolesRequest request) {
        requireUser(userId);
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));
        for (Long roleId : request.getRoleIds()) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        permissionAggregator.evict(userId);
        tokenVersionService.bump(userId);
    }

    @Transactional
    public void assignBranches(Long userId, AssignBranchesRequest request) {
        requireUser(userId);
        userBranchMapper.delete(
                Wrappers.<UserBranch>lambdaQuery().eq(UserBranch::getUserId, userId));
        for (Long branchId : request.getBranchIds()) {
            UserBranch ub = new UserBranch();
            ub.setUserId(userId);
            ub.setBranchId(branchId);
            userBranchMapper.insert(ub);
        }
        tokenVersionService.bump(userId);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(userRoleMapper.selectRoleCodesByUserId(user.getId()))
                .branchIds(userBranchMapper.selectBranchIdsByUserId(user.getId()))
                .build();
    }
}
