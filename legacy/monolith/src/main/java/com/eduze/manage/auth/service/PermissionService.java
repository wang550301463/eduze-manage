package com.eduze.manage.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.Permission;
import com.eduze.manage.auth.mapper.PermissionMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public Map<String, List<Permission>> listGroupedByModule() {
        List<Permission> permissions =
                permissionMapper.selectList(
                        Wrappers.<Permission>lambdaQuery()
                                .orderByAsc(Permission::getModule, Permission::getCode));
        Map<String, List<Permission>> grouped = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            grouped.computeIfAbsent(permission.getModule(), k -> new java.util.ArrayList<>())
                    .add(permission);
        }
        return grouped;
    }
}
