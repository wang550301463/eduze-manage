package com.eduze.manage.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.Role;
import com.eduze.manage.auth.dto.RoleRequest;
import com.eduze.manage.auth.dto.RoleResponse;
import com.eduze.manage.auth.mapper.RoleMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<RoleResponse> list() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .eq(Role::getTenantId, TenantContext.getTenantId())
                        .orderByAsc(Role::getCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        Role role = new Role();
        role.setTenantId(TenantContext.getTenantId());
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setIsBuiltin(0);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "角色编码已存在", ex);
        }
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = requireRole(id);
        ensureNotBuiltin(role);
        role.setCode(request.getCode());
        role.setName(request.getName());
        try {
            roleMapper.updateById(role);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "角色编码已存在", ex);
        }
        return toResponse(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = requireRole(id);
        ensureNotBuiltin(role);
        roleMapper.deleteById(id);
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void ensureNotBuiltin(Role role) {
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "内置角色不可修改或删除");
        }
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .isBuiltin(role.getIsBuiltin())
                .build();
    }
}
